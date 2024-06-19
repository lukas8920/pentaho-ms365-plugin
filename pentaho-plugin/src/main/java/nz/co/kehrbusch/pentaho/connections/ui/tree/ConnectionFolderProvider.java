package nz.co.kehrbusch.pentaho.connections.ui.tree;

import nz.co.kehrbusch.pentaho.connections.manage.ConnectionDetailsInterface;
import nz.co.kehrbusch.pentaho.connections.manage.MS365ConnectionManager;
import org.pentaho.di.base.AbstractMeta;
import org.pentaho.di.core.logging.LogChannel;
import org.pentaho.di.i18n.BaseMessages;
import org.pentaho.di.repository.Repository;
import org.pentaho.di.ui.core.gui.GUIResource;
import org.pentaho.di.ui.core.widget.tree.TreeNode;
import org.pentaho.di.ui.spoon.Spoon;
import org.pentaho.di.ui.spoon.tree.TreeFolderProvider;

import java.util.List;
import java.util.stream.Collectors;

public class ConnectionFolderProvider extends TreeFolderProvider {
    private static final Class<?> PKG = ConnectionFolderProvider.class;
    public static final String STRING_MS365_CONNECTIONS = BaseMessages.getString( PKG, "MS365ConnectionsTree.Title" );

    private final MS365ConnectionManager connectionManager;

    public ConnectionFolderProvider() {
        this.connectionManager = MS365ConnectionManager.getInstance(new LogChannel());
    }

    @Override
    public void refresh(AbstractMeta meta, TreeNode treeNode, String filter ) {
        List<String> connectionNames = this.connectionManager.getConnections()
                .stream()
                .map(ConnectionDetailsInterface::getConnectionName)
                .collect(Collectors.toList());
        for ( String name : connectionNames) {
            if ( !filterMatch( name, filter ) ) {
                continue;
            }
            super.createTreeNode( treeNode, name, GUIResource.getInstance().getImageSlaveTree() );
        }
    }

    @Override
    public String getTitle() {
        return STRING_MS365_CONNECTIONS;
    }

    @Override
    public void create( AbstractMeta meta, TreeNode parent ) {
        Spoon spoon = Spoon.getInstance();
        Repository repository = spoon.getRepository();
        if( repository != null && repository.getUserInfo() != null && repository.getUserInfo().isAdmin() != null
                && Boolean.FALSE.equals( repository.getUserInfo().isAdmin() ) ) {
            return;
        }
        this.connectionManager.initConnections();
        refresh( meta, createTreeNode( parent, getTitle(), getTreeImage() ), null );
    }
}
