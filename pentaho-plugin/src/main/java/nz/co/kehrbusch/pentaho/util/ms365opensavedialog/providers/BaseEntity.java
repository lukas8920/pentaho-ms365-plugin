package nz.co.kehrbusch.pentaho.util.ms365opensavedialog.providers;

import nz.co.kehrbusch.ms365.interfaces.ISharepointConnection;
import nz.co.kehrbusch.ms365.interfaces.entities.ISharepointFile;
import org.pentaho.di.plugins.fileopensave.api.providers.Directory;
import org.pentaho.di.plugins.fileopensave.api.providers.Entity;
import org.pentaho.di.plugins.fileopensave.api.providers.File;
import org.pentaho.di.plugins.fileopensave.api.providers.Providerable;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.logging.Logger;

public abstract class BaseEntity implements Entity, Providerable, File, ISharepointFile {
    private static final Logger log = Logger.getLogger(BaseEntity.class.getName());

    private String provider;
    private String type;
    private String root;
    private Date date;
    private boolean canEdit = false;
    private boolean canDelete = false;
    private BaseEntity parent = null;
    private int childrenCount;
    private boolean hasInitChildren = false;
    private int size;

    protected final List<BaseEntity> children = new ArrayList<>();
    
    @Override
    public String getProvider() {
        return provider;
    }

    @Override
    public int getChildrenCount() {
        return this.childrenCount;
    }

    public void setChildrenCount(int childrenCount) {
        this.childrenCount = childrenCount;
    }

    @Override
    public String getPath() {
        StringBuilder builder = new StringBuilder();
        if (parent == null){
            return "/sharepoint/";
        } else {
            builder.append("/");
            BaseEntity parentDirectory = getParentObject();
            while (parentDirectory != null){
                builder.insert(0, parentDirectory.getName());
                builder.insert(0, "/");
                parentDirectory = parentDirectory.getParentObject();
            }
            builder.insert(0, "/sharepoint");
            if (this instanceof Directory){
                builder.append(this.getName() + "/");
            }
            return builder.toString();
        }
    }

    public abstract void setId(String id);

    public BaseEntity getParentObject(){
        return this.parent;
    }

    public void setParent(BaseEntity ms365Object){
        this.parent = ms365Object;
    }

    @Override public String getParent() {
        return parent == null ? null : parent.getName();
    }

    @Override public String getType() {
        return type;
    }

    public void setType( String type ) {
        this.type = type;
    }

    @Override public String getRoot() {
        return root;
    }

    @Override public Date getDate() {
        return date;
    }

    @Override public boolean isCanEdit() {
        return canEdit;
    }

    @Override
    public boolean isCanDelete() {
        return canDelete;
    }

    public List<BaseEntity> getChildren(){
        return this.children;
    };

    public boolean hasInitChildren(){
        return this.hasInitChildren;
    }

    public void setHasInitChildren(boolean hasInitChildren) {
        this.hasInitChildren = hasInitChildren;
    }

    public abstract void getRemoteChildren(ISharepointConnection iSharepointConnection);

    public void addChild(BaseEntity baseEntity){
        this.children.add(baseEntity);
    }

    public abstract void setName(String name);

    public abstract String getName();

    public void setSize(int size) {
        this.size = size;
    }
    public int getSize(){
        return this.size;
    }
}
