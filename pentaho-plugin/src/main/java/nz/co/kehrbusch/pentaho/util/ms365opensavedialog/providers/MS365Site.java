package nz.co.kehrbusch.pentaho.util.ms365opensavedialog.providers;

import nz.co.kehrbusch.ms365.interfaces.ISharepointConnection;
import nz.co.kehrbusch.ms365.interfaces.entities.ISharepointFile;
import org.pentaho.di.plugins.fileopensave.api.providers.Directory;

import java.util.List;
import java.util.stream.Collectors;

public class MS365Site extends BaseEntity implements Directory {
    public static final int MAX_SITES_TO_FETCH = 100;

    private String id;
    private String name;
    private String tmpParentId;

    @Override
    public String getProvider(){
        return MS365FileProvider.TYPE;
    }

    @Override
    public String getId() {
        return this.id;
    }

    @Override
    public boolean isCanAddChildren() {
        return false;
    }

    @Override
    public boolean isHasChildren() {
        return true;
    }

    @Override
    public String getName() {
        return this.name;
    }

    @Override
    public void setName(String name) {
        this.name = name;
    }

    public void setId(String id) {
        this.id = id;
    }

    @Override
    public String getType(){
        return "Folder";
    }

    @Override
    public void getRemoteChildren(ISharepointConnection iSharepointConnection) {
        List<ISharepointFile> sharepointFiles = iSharepointConnection.getRootItems(this, MAX_SITES_TO_FETCH);
        List<BaseEntity> baseEntities = sharepointFiles.stream()
                .map(sharepointFile -> {
                    if (sharepointFile.getChildrenCount() > 0){
                        MS365Directory ms365Directory = new MS365Directory();
                        ms365Directory.setName(sharepointFile.getName());
                        ms365Directory.setId(sharepointFile.getId());
                        ms365Directory.setParent(MS365Site.this);
                        ms365Directory.setChildrenCount(sharepointFile.getChildrenCount());
                        return ms365Directory;
                    } else {
                        MS36File ms36File = new MS36File();
                        ms36File.setId(sharepointFile.getId());
                        ms36File.setName(sharepointFile.getName());
                        ms36File.setParent(MS365Site.this);
                        ms36File.setChildrenCount(sharepointFile.getChildrenCount());
                        return ms36File;
                    }
                })
                .collect(Collectors.toList());
        this.children.addAll(baseEntities);
    }

    public String getTmpParentId() {
        return tmpParentId;
    }

    public void setTmpParentId(String tmpParentId) {
        this.tmpParentId = tmpParentId;
    }
}
