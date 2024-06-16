package nz.co.kehrbusch.pentaho.util.ms365opensavedialog.providers;

import nz.co.kehrbusch.ms365.interfaces.ISharepointConnection;
import org.pentaho.di.plugins.fileopensave.api.providers.File;

public class MS36File extends BaseEntity implements File {
    private String name;
    private String id;

    @Override
    public String getProvider() {
        return MS365FileProvider.TYPE;
    }

    @Override
    public String getType(){
        return "File";
    }

    @Override
    public void getRemoteChildren(ISharepointConnection iSharepointConnection) {
        //nothing to do here
    }

    public void setId(String id){
        this.id = id;
    }

    public String getId(){
        return this.id;
    }

    public String getName(){
        return this.name;
    }

    public void setName(String name){
        this.name = name;
    }
}
