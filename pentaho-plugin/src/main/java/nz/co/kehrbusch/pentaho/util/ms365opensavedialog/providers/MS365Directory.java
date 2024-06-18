package nz.co.kehrbusch.pentaho.util.ms365opensavedialog.providers;

import nz.co.kehrbusch.ms365.interfaces.ISharepointConnection;
import nz.co.kehrbusch.ms365.interfaces.entities.ISharepointFile;
import org.pentaho.di.plugins.fileopensave.api.providers.Directory;

import java.util.List;
import java.util.stream.Collectors;

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

    @Override
    public void getRemoteChildren(ISharepointConnection iSharepointConnection) {
        List<ISharepointFile> sharepointFiles = iSharepointConnection.getChildren(this, getChildrenCount());
        List<BaseEntity> baseEntities = sharepointFiles.stream()
                .map(sharepointFile -> {
                    if (sharepointFile.getChildrenCount() > 0){
                        MS365Directory ms365Directory = new MS365Directory();
                        ms365Directory.setName(sharepointFile.getName());
                        ms365Directory.setId(sharepointFile.getId());
                        ms365Directory.setParent(MS365Directory.this);
                        ms365Directory.setChildrenCount(sharepointFile.getChildrenCount());
                        return ms365Directory;
                    } else {
                        MS365File ms36File = new MS365File();
                        ms36File.setId(sharepointFile.getId());
                        ms36File.setName(sharepointFile.getName());
                        ms36File.setParent(MS365Directory.this);
                        ms36File.setChildrenCount(sharepointFile.getChildrenCount());
                        return ms36File;
                    }
                })
                .collect(Collectors.toList());
        this.children.addAll(baseEntities);
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
