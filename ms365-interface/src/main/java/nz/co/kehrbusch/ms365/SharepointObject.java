package nz.co.kehrbusch.ms365;

import nz.co.kehrbusch.ms365.interfaces.entities.ISharepointFile;

import java.util.ArrayList;
import java.util.List;

public class SharepointObject implements ISharepointFile {
    private String id;
    private String name;
    private boolean hasChildren;
    private ISharepointFile parent;

    private final List<ISharepointFile> children;

    public SharepointObject(){
        this.children = new ArrayList<>();
    }

    public SharepointObject(String id, String name, ISharepointFile parent, boolean hasChildren){
        this.id = id;
        this.name = name;
        this.hasChildren = hasChildren;
        this.parent = parent;
        this.children = new ArrayList<>();
    }

    @Override
    public String getId() {
        return this.id;
    }

    @Override
    public String getName() {
        return this.name;
    }

    @Override
    public boolean hasChildren() {
        return this.hasChildren;
    }

    @Override
    public ISharepointFile getParentObject() {
        return this.parent;
    }

    @Override
    public List<ISharepointFile> getChildren() {
        return this.children;
    }

    public void addChildren(List<ISharepointFile> iSharepointFiles){
        this.children.addAll(iSharepointFiles);
    }

    public void addChild(ISharepointFile iSharepointFile){this.children.add(iSharepointFile);}

    public void setParent(ISharepointFile iSharepointFile){
        this.parent = iSharepointFile;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setHasChildren(boolean hasChildren) {
        this.hasChildren = hasChildren;
    }

    public void setName(String name) {
        this.name = name;
    }
}
