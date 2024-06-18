package nz.co.kehrbusch.ms365.interfaces;

import nz.co.kehrbusch.ms365.interfaces.entities.ISharepointFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.InvalidPathException;
import java.util.List;

public interface ISharepointConnection {
    //Call drives first, if only one, then ISharepointConnection automatically returns the next level
    List<ISharepointFile> getChildren(ISharepointFile iSharepointFile, int maxNrOfResults);
    List<ISharepointFile> getSites(int maxNrOfResults);
    List<ISharepointFile> getRootItems(ISharepointFile parent, int maxNrOfResults);
    InputStream getInputStream(ISharepointFile iSharepointFile) throws IOException;
    ISharepointFile inflateTreeByPath(String path) throws InvalidPathException;
}
