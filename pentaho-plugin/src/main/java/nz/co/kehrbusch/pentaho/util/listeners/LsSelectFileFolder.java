package nz.co.kehrbusch.pentaho.util.listeners;

import nz.co.kehrbusch.pentaho.connections.manage.GraphConnectionDetails;
import nz.co.kehrbusch.pentaho.connections.manage.MS365ConnectionManager;
import nz.co.kehrbusch.pentaho.trans.textfileinput.MS365TextFileInputDialog;
import nz.co.kehrbusch.pentaho.util.ms365opensavedialog.MS365OpenSaveDialog;
import nz.co.kehrbusch.pentaho.util.ms365opensavedialog.providers.*;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CCombo;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Shell;
import org.pentaho.di.core.Const;
import org.pentaho.di.core.logging.LogChannel;
import org.pentaho.di.i18n.BaseMessages;
import org.pentaho.di.plugins.fileopensave.service.ProviderServiceService;
import org.pentaho.di.ui.core.FileDialogOperation;
import org.pentaho.di.ui.core.widget.TextVar;

public class LsSelectFileFolder implements SelectionListener {
    private static final Class<?> PKG = MS365TextFileInputDialog.class;
    private static final int WIDTH = ( Const.isOSX() || Const.isLinux() ) ? 930 : 947;
    private static final int HEIGHT = ( Const.isOSX() || Const.isLinux() ) ? 618 : 626;

    private final FileFolderInput fileFolderInput;
    private final MS365ConnectionManager connectionManager;

    public LsSelectFileFolder(FileFolderInput fileFolderInput){
        this.fileFolderInput = fileFolderInput;
        this.connectionManager = MS365ConnectionManager.getInstance(new LogChannel());
    }

    @Override
    public void widgetSelected(SelectionEvent selectionEvent) {
        if (fileFolderInput.getConnections().length > 0 && fileFolderInput.getwConnectionField().getText().length() > 0){
            GraphConnectionDetails graphConnectionDetails = (GraphConnectionDetails) connectionManager.provideDetailsByConnectionName(fileFolderInput.getwConnectionField().getText());

            MS365OpenSaveDialog ms365OpenSaveDialog = new MS365OpenSaveDialog(fileFolderInput.getShell(), WIDTH, HEIGHT, new LogChannel());
            ProviderServiceService.get()
                    .add(new MS365FileProvider(graphConnectionDetails.getISharepointConnection(),
                            () -> ms365OpenSaveDialog.refreshDisplay(false),
                            ms365OpenSaveDialog::setLoadingVisibility));
            FileDialogOperation fileDialogOperation = new FileDialogOperation(FileDialogOperation.SELECT_FILE_FOLDER);
            fileDialogOperation.setProvider(MS365FileProvider.TYPE);

            ms365OpenSaveDialog.setProviderFilter(MS365FileProvider.TYPE);
            ms365OpenSaveDialog.setProvider(MS365FileProvider.TYPE);
            ms365OpenSaveDialog.open(fileDialogOperation);

            if (ms365OpenSaveDialog.getSelectedFile() != null){
                BaseEntity baseEntity = ms365OpenSaveDialog.getSelectedFile();
                if (baseEntity != null){
                    fileFolderInput.setSelectedEntity(baseEntity);
                }
                if (baseEntity instanceof MS365File) {
                    fileFolderInput.getFilename().setText(ms365OpenSaveDialog.getSelectedFile().getPath() + ms365OpenSaveDialog.getSelectedFile().getName());
                    //ms365TextFileInputDialog.getWGet().setVisible(true);
                } else if (baseEntity instanceof MS365Directory || baseEntity instanceof MS365Site){
                    fileFolderInput.getFilename().setText(ms365OpenSaveDialog.getSelectedFile().getPath());
                }
            }
        } else if (!(fileFolderInput.getConnections().length > 0)){
            MessageBox mb = new MessageBox(fileFolderInput.getShell(), SWT.OK | SWT.ICON_ERROR);
            mb.setMessage(BaseMessages.getString(PKG, "MS365CsvInput.NoConnectionCreated.Error"));
            mb.open();
        } else {
            MessageBox mb = new MessageBox(fileFolderInput.getShell(), SWT.OK | SWT.ICON_ERROR);
            mb.setMessage(BaseMessages.getString(PKG, "MS365CsvInput.NoConnectionChosen.Error"));
            mb.open();
        }
    }

    @Override
    public void widgetDefaultSelected(SelectionEvent selectionEvent) {

    }

    public interface FileFolderInput {
        Shell getShell();
        void setSelectedEntity(BaseEntity baseEntity);
        String[] getConnections();
        TextVar getFilename();
        CCombo getwConnectionField();
    }
}
