package nz.co.kehrbusch.ms365.interfaces;

import nz.co.kehrbusch.ms365.interfaces.entities.ISharepointFile;

import java.util.List;

public interface ISharepointConnection {
    //Call drives first, if only one, then ISharepointConnection automatically returns the next level
    List<ISharepointFile> getDrives(boolean maxNrOfResults);
    List<ISharepointFile> getRootFiles(boolean maxNrOfResuls);
    List<ISharepointFile> getChildren(ISharepointFile iSharepointFile, boolean maxNrOfResults);
}
