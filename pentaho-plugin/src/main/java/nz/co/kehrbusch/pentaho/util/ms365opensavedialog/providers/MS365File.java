package nz.co.kehrbusch.pentaho.util.ms365opensavedialog.providers;

import nz.co.kehrbusch.ms365.interfaces.ISharepointConnection;
import nz.co.kehrbusch.ms365.interfaces.entities.ISharepointFile;
import nz.co.kehrbusch.ms365.interfaces.entities.IStreamProvider;
import org.pentaho.di.plugins.fileopensave.api.providers.File;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDateTime;

public class MS365File extends BaseEntity implements File, IStreamProvider {
    public static final int MAX_Files_TO_FETCH = 100;
    public static final String NAME_OF_ROOT = "root";

    private String name;
    private String id;
    private InputStream inputStream;
    private LocalDateTime createdDate;
    private LocalDateTime lastModifiedDate;
    private String webUrl;

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

    public String getExtension(){
        String[] split = getName().split("\\.");
        if (split.length > 1){
            return split[1];
        }
        return  "";
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

    public void clearRootDirectory(){
        BaseEntity baseEntity = this;
        while (baseEntity.getParentObject() != null){
            if (baseEntity.getParentObject().getName().equals(NAME_OF_ROOT)){
                BaseEntity root = baseEntity.getParentObject();
                baseEntity.setParent(root.getParentObject());
                baseEntity.getParentObject().removeChild(root);
                baseEntity.getParentObject().addChild(this);
                break;
            }
            baseEntity = baseEntity.getParentObject();
        }
    }

    public void disposeInputStream() throws IOException {
        if (this.inputStream != null){
            this.inputStream.close();
            this.inputStream = null;
        }
    }

    public LocalDateTime getCreatedDate() {
        return createdDate;
    }

    public void setCreatedDate(LocalDateTime createdDate) {
        this.createdDate = createdDate;
    }

    public LocalDateTime getLastModifiedDate() {
        return lastModifiedDate;
    }

    public void setLastModifiedDate(LocalDateTime lastModifiedDate) {
        this.lastModifiedDate = lastModifiedDate;
    }

    public String getWebUrl() {
        return webUrl;
    }

    public void setWebUrl(String webUrl) {
        this.webUrl = webUrl;
    }
}
