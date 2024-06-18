package nz.co.kehrbusch.ms365.interfaces.entities;

public interface ISharepointFile {
    String getId();
    String getName();
    ISharepointFile getParentObject();
    int getChildrenCount();
    int getSize();
}
