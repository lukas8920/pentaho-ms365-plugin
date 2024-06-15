package nz.co.kehrbusch.ms365.interfaces.entities;

import java.util.List;

public interface ISharepointFile {
    String getId();
    String getName();
    boolean hasChildren();
    ISharepointFile getParentObject();
    List<ISharepointFile> getChildren();
}
