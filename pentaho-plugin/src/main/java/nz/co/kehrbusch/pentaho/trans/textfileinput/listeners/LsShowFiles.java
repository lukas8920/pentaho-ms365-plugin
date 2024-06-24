package nz.co.kehrbusch.pentaho.trans.textfileinput.listeners;

import nz.co.kehrbusch.pentaho.trans.textfileinput.MS365TextFileInputDialog;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;

public class LsShowFiles implements Listener {
    private final MS365TextFileInputDialog ms365TextFileInputDialog;

    public LsShowFiles(MS365TextFileInputDialog ms365TextFileInputDialog){
        this.ms365TextFileInputDialog = ms365TextFileInputDialog;
    }

    @Override
    public void handleEvent(Event event) {
        ms365TextFileInputDialog.getLblLoadingInfo().setVisible(true);
        ms365TextFileInputDialog.getWbShowFiles().setEnabled(false);
        ms365TextFileInputDialog.handleAfterFilesAvailable(((iSharepointConnection, wrapper) -> {
            ms365TextFileInputDialog.showFiles(wrapper);
            ms365TextFileInputDialog.getWbShowFiles().setEnabled(true);
        }));
    }
}
