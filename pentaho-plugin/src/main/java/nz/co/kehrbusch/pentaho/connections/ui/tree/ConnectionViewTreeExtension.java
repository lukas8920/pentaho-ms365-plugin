package nz.co.kehrbusch.pentaho.connections.ui.tree;

import nz.co.kehrbusch.pentaho.connections.ui.dialog.ConnectionDelegate;
import org.pentaho.di.core.exception.KettleException;
import org.pentaho.di.core.extension.ExtensionPoint;
import org.pentaho.di.core.extension.ExtensionPointInterface;
import org.pentaho.di.core.logging.LogChannelInterface;
import org.pentaho.di.ui.spoon.SelectionTreeExtension;
import org.pentaho.di.ui.spoon.Spoon;

@ExtensionPoint( id = "MS365ConnectionViewTreeExtension", description = "",
        extensionPointId = "SpoonViewTreeExtension" )
public class ConnectionViewTreeExtension implements ExtensionPointInterface {
    private ConnectionDelegate connectionDelegate;

    public ConnectionViewTreeExtension() {
        this.connectionDelegate = ConnectionDelegate.getInstance();
    }

    @Override
    public void callExtensionPoint(LogChannelInterface log, Object object ) throws KettleException {
        SelectionTreeExtension selectionTreeExtension = (SelectionTreeExtension) object;
        if ( selectionTreeExtension.getAction().equals( Spoon.EDIT_SELECTION_EXTENSION ) ) {
            if ( selectionTreeExtension.getSelection() instanceof ConnectionTreeItem ) {
                ConnectionTreeItem connectionTreeItem = (ConnectionTreeItem) selectionTreeExtension.getSelection();
                connectionDelegate.openDialog( connectionTreeItem.getLabel() );
            }
        }
    }
}
