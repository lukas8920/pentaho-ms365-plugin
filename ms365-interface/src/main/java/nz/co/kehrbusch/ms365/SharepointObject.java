package nz.co.kehrbusch.ms365;

import nz.co.kehrbusch.ms365.interfaces.entities.Counter;
import nz.co.kehrbusch.ms365.interfaces.entities.ICountableSharepointFile;
import nz.co.kehrbusch.ms365.interfaces.entities.ISharepointFile;

import java.util.ArrayList;
import java.util.List;

public class SharepointObject implements ICountableSharepointFile {
    private String id;
    private String name;
    private ISharepointFile parent;
    private int childrenCount = 1;
    private int size;
    private Counter fileCounter;
    private Counter partCounter;

    private final List<ISharepointFile> children;

    public SharepointObject(){
        this.children = new ArrayList<>();
    }

    public SharepointObject(String id, String name, ISharepointFile parent, int childrenCount){
        this.id = id;
        this.name = name;
        this.childrenCount = childrenCount;
        this.parent = parent;
        this.children = new ArrayList<>();
    }

    public SharepointObject(String id, String name, ISharepointFile parent){
        this.id = id;
        this.name = name;
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
    public ISharepointFile getParentObject() {
        return this.parent;
    }

    @Override
    public int getChildrenCount() {
        return this.childrenCount;
    }

    @Override
    public int getSize() {
        return this.size;
    }

    public void setSize(int size){
        this.size = size;
    }

    public void setChildrenCount(int childrenCount){
        this.childrenCount = childrenCount;
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

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public Counter getFileCounter() {
        return this.fileCounter;
    }

    @Override
    public void setFileCounter(Counter fileCounter) {
        this.fileCounter = fileCounter;
    }

    @Override
    public Counter getPartCounter() {
        return this.partCounter;
    }

    @Override
    public void setPartCounter(Counter partCounter) {
        this.partCounter = partCounter;
    }
}
