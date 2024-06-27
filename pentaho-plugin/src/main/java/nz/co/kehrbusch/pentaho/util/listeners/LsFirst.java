package nz.co.kehrbusch.pentaho.util.listeners;

import nz.co.kehrbusch.pentaho.trans.textfileinput.MS365TextFileInputDialog;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;

public class LsFirst implements Listener {
    private final MS365TextFileInputDialog ms365TextFileInputDialog;

    public LsFirst(MS365TextFileInputDialog ms365TextFileInputDialog){
        this.ms365TextFileInputDialog = ms365TextFileInputDialog;
    }

    @Override
    public void handleEvent(Event event) {
        ms365TextFileInputDialog.getLblLoadingInfo().setVisible(true);
        ms365TextFileInputDialog.getWbFirst().setEnabled(false);
        Handler.handleAfterFilesAvailable(((iSharepointConnection, wrapper) -> {
            ms365TextFileInputDialog.first(iSharepointConnection, wrapper, false);
            ms365TextFileInputDialog.getWbFirst().setEnabled(true);
        }), this.ms365TextFileInputDialog);
    }
}
