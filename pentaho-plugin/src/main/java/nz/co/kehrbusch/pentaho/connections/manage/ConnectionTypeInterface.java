package nz.co.kehrbusch.pentaho.connections.manage;

import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;

//Implementations need to be singletons
public interface ConnectionTypeInterface {
    String provideType();
    //provides an empty Instance for the details of this type
    ConnectionDetailsInterface createInstanceOfDetails();
    GraphConnectionType.GraphConnectionTypeComposite openDialog(Display display, UITypeCallback changeCallback, Composite wTabFolder, int width);
    void closeDialog();
    void insertUIValues(ConnectionDetailsInterface connectionDetailsInterface);
}
