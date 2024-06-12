package nz.co.kehrbusch.pentaho.connections.ui.lifecycle;

import nz.co.kehrbusch.pentaho.connections.ui.tree.ConnectionFolderProvider;
import org.pentaho.di.core.annotations.LifecyclePlugin;
import org.pentaho.di.core.lifecycle.LifeEventHandler;
import org.pentaho.di.core.lifecycle.LifecycleException;
import org.pentaho.di.core.lifecycle.LifecycleListener;
import org.pentaho.di.ui.spoon.Spoon;

import java.util.function.Supplier;

@LifecyclePlugin( id = "MS365ConnectionLifecycleListener" )
public class ConnectionLifecycleListener implements LifecycleListener {
    private Supplier<Spoon> spoonSupplier = Spoon::getInstance;

    @Override
    public void onStart( LifeEventHandler handler ) throws LifecycleException {
        Spoon spoon = spoonSupplier.get();
        if ( spoon != null ) {
            spoon.getTreeManager()
                    .addTreeProvider( Spoon.STRING_TRANSFORMATIONS, new ConnectionFolderProvider());
            spoon.getTreeManager().addTreeProvider( Spoon.STRING_JOBS, new ConnectionFolderProvider());
        }
    }

    @Override
    public void onExit( LifeEventHandler handler ) throws LifecycleException {

    }
}
