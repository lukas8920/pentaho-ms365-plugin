package nz.co.kehrbusch.pentaho.connections.ui.tree;

import nz.co.kehrbusch.pentaho.connections.ui.dialog.ConnectionDelegate;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.Tree;
import org.pentaho.di.core.exception.KettleException;
import org.pentaho.di.core.extension.ExtensionPoint;
import org.pentaho.di.core.extension.ExtensionPointInterface;
import org.pentaho.di.core.logging.LogChannelInterface;
import org.pentaho.di.i18n.BaseMessages;
import org.pentaho.di.ui.core.ConstUI;
import org.pentaho.di.ui.spoon.Spoon;
import org.pentaho.di.ui.spoon.TreeSelection;

import java.util.function.Supplier;
import java.util.logging.Logger;

@ExtensionPoint( id = "MS365ConnectionPopupMenuExtension", description = "Creates popup menus for MS365 Connections",
        extensionPointId = "SpoonPopupMenuExtension" )
public class ConnectionPopupMenuExtension implements ExtensionPointInterface {
    private static final Logger log = Logger.getLogger(ConnectionPopupMenuExtension.class.getName());

    private static final Class<?> PKG = ConnectionPopupMenuExtension.class;

    private Supplier<Spoon> spoonSupplier = Spoon::getInstance;
    private Menu rootMenu;
    private Menu itemMenu;
    private ConnectionDelegate ms365ConnectionDelegate;
    private ConnectionTreeItem ms365ConnectionTreeItem;

    public ConnectionPopupMenuExtension() {
        this.ms365ConnectionDelegate = ConnectionDelegate.getInstance();
    }

    @Override
    public void callExtensionPoint(LogChannelInterface logChannelInterface, Object extension )
            throws KettleException {
        Menu popupMenu = null;

        Tree selectionTree = (Tree) extension;
        TreeSelection[] objects = spoonSupplier.get().getTreeObjects( selectionTree );
        TreeSelection object = objects[ 0 ];
        Object selection = object.getSelection();

        if ( selection == Object.class ) {
            popupMenu = createRootPopupMenu( selectionTree );
        } else if ( selection instanceof ConnectionTreeItem ) {
            ms365ConnectionTreeItem = (ConnectionTreeItem) selection;
            popupMenu = createItemPopupMenu( selectionTree );
        }

        if ( popupMenu != null ) {
            ConstUI.displayMenu( popupMenu, selectionTree );
        } else {
            selectionTree.setMenu( null );
        }
    }

    private Menu createRootPopupMenu( Tree tree ) {
        if ( rootMenu == null ) {
            rootMenu = new Menu( tree );
            MenuItem menuItem = new MenuItem( rootMenu, SWT.NONE );
            menuItem.setText( BaseMessages.getString( PKG, "MS365ConnectionPopupMenuExtension.MenuItem.New" ) );
            menuItem.addSelectionListener( new SelectionAdapter() {
                @Override
                public void widgetSelected( SelectionEvent selectionEvent ) {
                    ms365ConnectionDelegate.openDialog();
                }
            } );
        }
        return rootMenu;
    }

    private Menu createItemPopupMenu( Tree tree ) {
        if ( itemMenu == null ) {
            itemMenu = new Menu( tree );
            MenuItem editMenuItem = new MenuItem( itemMenu, SWT.NONE );
            editMenuItem.setText( BaseMessages.getString( PKG, "MS365ConnectionPopupMenuExtension.MenuItem.Edit" ) );
            editMenuItem.addSelectionListener( new SelectionAdapter() {
                @Override public void widgetSelected( SelectionEvent selectionEvent ) {
                    ms365ConnectionDelegate.openDialog( ms365ConnectionTreeItem.getLabel() );
                }
            } );

            MenuItem deleteMenuItem = new MenuItem( itemMenu, SWT.NONE );
            deleteMenuItem.setText( BaseMessages.getString( PKG, "MS365ConnectionPopupMenuExtension.MenuItem.Delete" ) );
            deleteMenuItem.addSelectionListener( new SelectionAdapter() {
                @Override public void widgetSelected( SelectionEvent selectionEvent ) {
                    ms365ConnectionDelegate.delete( ms365ConnectionTreeItem.getLabel() );
                }
            } );
        }
        return itemMenu;
    }
}
