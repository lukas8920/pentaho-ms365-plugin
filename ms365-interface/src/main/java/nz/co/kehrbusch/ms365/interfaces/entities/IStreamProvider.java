package nz.co.kehrbusch.ms365.interfaces.entities;

import nz.co.kehrbusch.ms365.interfaces.ISharepointConnection;

import java.io.IOException;
import java.io.InputStream;

public interface IStreamProvider extends ISharepointFile {
    InputStream getInputStream(ISharepointConnection iSharepointConnection) throws IOException;
    void disposeInputStream() throws IOException;
    String getPath();
}
