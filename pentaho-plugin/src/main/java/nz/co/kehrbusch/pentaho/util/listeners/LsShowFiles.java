package nz.co.kehrbusch.pentaho.util.listeners;

import nz.co.kehrbusch.pentaho.util.file.SharepointFileWrapper;
import org.eclipse.swt.widgets.*;
import org.pentaho.di.i18n.BaseMessages;
import org.pentaho.di.trans.step.BaseStepMeta;
import org.pentaho.di.ui.core.dialog.EnterSelectionDialog;

public class LsShowFiles<T extends BaseStepMeta> implements Listener {
    private final ShowFilesInput<T> showFilesInput;

    public LsShowFiles(ShowFilesInput<T> showFilesInput){
        this.showFilesInput = showFilesInput;
    }

    @Override
    public void handleEvent(Event event) {
        this.showFilesInput.getLblLoadingInfo().setVisible(true);
        this.showFilesInput.getWbShowFiles().setEnabled(false);
        Handler.handleAfterFilesAvailable(((iSharepointConnection, wrapper) -> {
            showFiles(wrapper);
            this.showFilesInput.getWbShowFiles().setEnabled(true);
        }), this.showFilesInput);
    }

    public void showFiles(SharepointFileWrapper<T> wrapper) {
        String[] files = wrapper.getStreamProviders().stream().map(file -> file.getPath() + file.getName()).toArray(String[]::new);
        if (files.length > 0) {
            EnterSelectionDialog esd = new EnterSelectionDialog(showFilesInput.getShell(), files, "Files read", "Files read:");
            esd.setViewOnly();
            esd.open();
        } else {
            MessageBox mb = new MessageBox(showFilesInput.getShell(), 33);
            mb.setMessage(BaseMessages.getString(showFilesInput.getPackage(), "MS365ShowFilesDialog.NoFilesFound.DialogMessage", new String[0]));
            mb.setText(BaseMessages.getString(showFilesInput.getPackage(), "System.Dialog.Error.Title", new String[0]));
            mb.open();
        }
    }

    public interface ShowFilesInput<T extends BaseStepMeta> extends Handler.HandlerInput<T> {
        Shell getShell();
        Button getWbShowFiles();
        Class<?> getPackage();
    }
}
