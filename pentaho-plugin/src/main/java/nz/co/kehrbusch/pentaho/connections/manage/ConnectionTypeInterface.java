package nz.co.kehrbusch.pentaho.connections.manage;

import nz.co.kehrbusch.pentaho.connections.ui.dialog.MS365DetailsCompositeHelper;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.pentaho.di.core.logging.LogChannelInterface;

//Implementations need to be singletons
public interface ConnectionTypeInterface {
    String provideType();
    //provides an empty Instance for the details of this type
    ConnectionDetailsInterface createInstanceOfDetails();
    GraphConnectionType.GraphConnectionTypeComposite openDialog(Display display, MS365DetailsCompositeHelper helper, UITypeCallback changeCallback, Composite wTabFolder, int width);
    void closeDialog();
    void insertUIValues(ConnectionDetailsInterface connectionDetailsInterface);
    LogChannelInterface getLog();
}
