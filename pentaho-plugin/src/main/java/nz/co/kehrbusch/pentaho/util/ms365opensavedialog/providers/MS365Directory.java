package nz.co.kehrbusch.pentaho.util.ms365opensavedialog.providers;

import org.pentaho.di.plugins.fileopensave.api.providers.Directory;

public class MS365Directory extends BaseEntity implements Directory {
    private String id;
    private String name;

    @Override
    public String getProvider(){
        return MS365FileProvider.TYPE;
    }

    @Override
    public boolean isCanAddChildren() {
        return false;
    }

    @Override
    public boolean isHasChildren() {
        return Directory.super.isHasChildren();
    }

    @Override
    public String getType(){
        return "Folder";
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
