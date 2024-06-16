package nz.co.kehrbusch.ms365.interfaces;

import nz.co.kehrbusch.ms365.interfaces.entities.ISharepointFile;

import java.util.List;

public interface ISharepointConnection {
    //Call drives first, if only one, then ISharepointConnection automatically returns the next level
    List<ISharepointFile> getChildren(ISharepointFile iSharepointFile, int maxNrOfResults);
    List<ISharepointFile> getSites(int maxNrOfResults);
    List<ISharepointFile> getRootItems(ISharepointFile parent, int maxNrOfResults);
}
