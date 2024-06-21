package nz.co.kehrbusch.pentaho.util.file;

import nz.co.kehrbusch.ms365.interfaces.ISharepointConnection;
import nz.co.kehrbusch.ms365.interfaces.entities.ISharepointFile;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class GraphOutputStream extends BufferedOutputStream {
    private final ByteArrayOutputStream byteArrayOutputStream;
    private final ISharepointConnection iSharepointConnection;
    private final ISharepointFile iSharepointFile;
    private final boolean append;

    public GraphOutputStream(ISharepointConnection iSharepointConnection, ByteArrayOutputStream byteArrayOutputStream, ISharepointFile iSharepointFile, boolean append){
        super(byteArrayOutputStream);
        this.byteArrayOutputStream = byteArrayOutputStream;
        this.iSharepointConnection = iSharepointConnection;
        this.iSharepointFile = iSharepointFile;
        this.append = append;
    }

    @Override
    public void write(int b) throws IOException {
        byteArrayOutputStream.write(b);
    }

    @Override
    public void write(byte[] b, int off, int len) throws IOException {
        byteArrayOutputStream.write(b, off, len);
    }

    @Override
    public void flush() throws IOException {
        super.flush();
        byte[] data = byteArrayOutputStream.toByteArray();
        byteArrayOutputStream.reset();
        this.iSharepointConnection.writeToSharepoint(this.iSharepointFile, data, append);
    }

    @Override
    public void close() throws IOException {
        flush();
        byteArrayOutputStream.close();
        super.close();
    }


    public ISharepointFile getiSharepointFile() {
        return iSharepointFile;
    }
}
