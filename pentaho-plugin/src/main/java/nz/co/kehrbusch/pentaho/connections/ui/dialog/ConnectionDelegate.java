package nz.co.kehrbusch.pentaho.connections.ui.dialog;

import nz.co.kehrbusch.pentaho.connections.manage.MS365ConnectionManager;
import nz.co.kehrbusch.pentaho.connections.ui.tree.ConnectionFolderProvider;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.MessageBox;
import org.pentaho.di.base.AbstractMeta;
import org.pentaho.di.core.EngineMetaInterface;
import org.pentaho.di.core.logging.LogChannel;
import org.pentaho.di.i18n.BaseMessages;
import org.pentaho.di.repository.filerep.KettleFileRepository;
import org.pentaho.di.ui.spoon.Spoon;

import java.util.function.Supplier;

public class ConnectionDelegate {
    private static final Class<?> PKG = ConnectionDelegate.class;
    private final Supplier<Spoon> spoonSupplier = Spoon::getInstance;

    private static final int WIDTH = 630;
    private static final int HEIGHT = 630;
    private static ConnectionDelegate instance;

    private ConnectionDelegate() {
        // no-op
    }

    public static ConnectionDelegate getInstance() {
        if ( null == instance ) {
            instance = new ConnectionDelegate();
        }
        return instance;
    }

    public void openDialog() {
        if (spoonSupplier.get().getRepository() instanceof KettleFileRepository){
            ConnectionDialog connectionDialog = new ConnectionDialog( spoonSupplier.get().getShell(), WIDTH, HEIGHT );
            connectionDialog.open( BaseMessages.getString( PKG, "ConnectionDialog.dialog.new.title" ) );
        } else {
            generatePluginNotSupportedMessage();
        }
    }

    public void openDialog( String label ) {
        if (spoonSupplier.get().getRepository() instanceof KettleFileRepository){
            ConnectionDialog connectionDialog = new ConnectionDialog( spoonSupplier.get().getShell(), WIDTH, HEIGHT );
            connectionDialog.open( BaseMessages.getString( PKG, "ConnectionDialog.dialog.edit.title" ), label );
        } else {
            generatePluginNotSupportedMessage();
        }
    }

    public void delete(String label){
        DeleteDialog connectionDeleteDialog = new DeleteDialog(spoonSupplier.get().getShell());
        int flag;
        if (spoonSupplier.get().getRepository() instanceof KettleFileRepository){
            flag = connectionDeleteDialog.open(label);
        } else {
            generatePluginNotSupportedMessage();
            return;
        }
        if (flag == SWT.YES) {
            MS365ConnectionManager connectionManager = MS365ConnectionManager.getInstance(new LogChannel());
            connectionManager.delete(label);
            spoonSupplier.get().getShell().getDisplay().asyncExec(() -> spoonSupplier.get().refreshTree(
                    ConnectionFolderProvider.STRING_MS365_CONNECTIONS));
            EngineMetaInterface engineMetaInterface = spoonSupplier.get().getActiveMeta();
            if (engineMetaInterface instanceof AbstractMeta) {
                ((AbstractMeta) engineMetaInterface).setChanged();
            }
        }
    }

    public static void refreshMenu() {
        Supplier<Spoon> spoonSupplier = Spoon::getInstance;
        spoonSupplier.get().getShell().getDisplay().asyncExec( () -> spoonSupplier.get().refreshTree(
                ConnectionFolderProvider.STRING_MS365_CONNECTIONS ) );
        EngineMetaInterface engineMetaInterface = spoonSupplier.get().getActiveMeta();
        if ( engineMetaInterface instanceof AbstractMeta ) {
            ( (AbstractMeta) engineMetaInterface ).setChanged();
        }
    }

    private void generatePluginNotSupportedMessage(){
        Spoon spoon = spoonSupplier.get();
        MessageBox mb = new MessageBox(spoon.getShell(), SWT.OK | SWT.ICON_ERROR);
        mb.setMessage(BaseMessages.getString(PKG, "ConnectionFolder.Menu.RepositoryFailed"));
        mb.open();
    }
}
