package nz.co.kehrbusch.pentaho.util.ms365opensavedialog.providers;

import org.pentaho.di.plugins.fileopensave.api.providers.Entity;
import org.pentaho.di.plugins.fileopensave.api.providers.File;
import org.pentaho.di.plugins.fileopensave.api.providers.Providerable;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.logging.Logger;

public abstract class BaseEntity implements Entity, Providerable, File {
    private static final Logger log = Logger.getLogger(BaseEntity.class.getName());

    private String provider;
    private String type;
    private String root;
    private Date date;
    private boolean canEdit = false;
    private boolean canDelete = false;
    private MS365Directory parent = null;

    private final List<BaseEntity> children = new ArrayList<>();
    
    @Override public String getProvider() {
        return provider;
    }

    public void setProvider( String provider ) {
        this.provider = provider;
    }

    @Override public abstract String getName();

    public abstract void setName( String name );

    @Override public String getPath() {
        StringBuilder builder = new StringBuilder();
        if (parent == null){
            return "/sharepoint/";
        } else {
            builder.append("/");
            MS365Directory parentDirectory = getParentObject();
            while (parentDirectory != null){
                builder.insert(0, parentDirectory.getName());
                builder.insert(0, "/");
                parentDirectory = parentDirectory.getParentObject();
            }
            builder.insert(0, "/sharepoint");
            return builder.toString();
        }
    }

    public MS365Directory getParentObject(){
        return this.parent;
    }

    public void setParent(MS365Directory ms365Object){
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

    public void setRoot( String root ) {
        this.root = root;
    }

    @Override public Date getDate() {
        return date;
    }

    public void setDate( Date date ) {
        this.date = date;
    }

    @Override public boolean isCanEdit() {
        return canEdit;
    }

    public void setCanEdit( boolean canEdit ) {
        this.canEdit = canEdit;
    }

    @Override
    public boolean isCanDelete() {
        return canDelete;
    }

    public void setCanDelete( boolean canDelete ) {
        this.canDelete = canDelete;
    }

    public List<BaseEntity> getChildren(){
        log.info("Retrieve children from BaseEntity");
        return this.children;
    }

    public void deleteChild(BaseEntity ms365Object){
        this.children.remove(ms365Object);
    }

    public boolean hasChild(BaseEntity ms365Object){
        return this.children.contains(ms365Object);
    }

    public void addChild(BaseEntity ms365Object){
        this.children.add(ms365Object);
    }
}
