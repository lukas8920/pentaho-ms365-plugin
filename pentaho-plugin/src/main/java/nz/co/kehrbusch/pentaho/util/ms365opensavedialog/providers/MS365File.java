package nz.co.kehrbusch.pentaho.util.ms365opensavedialog.providers;

import nz.co.kehrbusch.ms365.interfaces.ISharepointConnection;
import nz.co.kehrbusch.ms365.interfaces.entities.IStreamProvider;
import org.pentaho.di.plugins.fileopensave.api.providers.File;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class MS365File extends BaseEntity implements File, IStreamProvider {
    private String name;
    private String id;
    private InputStream inputStream;

    @Override
    public String getProvider() {
        return MS365FileProvider.TYPE;
    }

    @Override
    public String getType(){
        return "File";
    }

    @Override
    public void getRemoteChildren(ISharepointConnection iSharepointConnection) {
        //nothing to do here
    }

    public void setId(String id){
        this.id = id;
    }

    public String getId(){
        return this.id;
    }

    public String getName(){
        return this.name;
    }

    public void setName(String name){
        this.name = name;
    }

    @Override
    public InputStream getInputStream(ISharepointConnection iSharepointConnection) throws IOException {
        if (inputStream == null){
            this.inputStream = iSharepointConnection.getInputStream(this);
        }
        // Read the existing input stream into a byte array
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        byte[] data = new byte[1024];
        int nRead;
        while ((nRead = inputStream.read(data, 0, data.length)) != -1) {
            buffer.write(data, 0, nRead);
        }
        buffer.flush();

        // Get the byte array
        byte[] byteArray = buffer.toByteArray();

        // Create a new input stream from the byte array
        InputStream newInputStream = new ByteArrayInputStream(byteArray);

        // Optionally, you can reset the old input stream if it supports mark/reset
        if (inputStream.markSupported()) {
            inputStream.reset();
        } else {
            // If mark/reset is not supported, you will have to use the newInputStream
            inputStream = new ByteArrayInputStream(byteArray);
        }
        return newInputStream;
    }

    public void disposeInputStream() throws IOException {
        if (this.inputStream != null){
            this.inputStream.close();
            this.inputStream = null;
        }
    }
}
