package nz.co.kehrbusch.pentaho.trans.textfileoutput;

import nz.co.kehrbusch.ms365.interfaces.ISharepointConnection;
import nz.co.kehrbusch.ms365.interfaces.entities.ISharepointFile;
import nz.co.kehrbusch.pentaho.connections.manage.GraphConnectionDetails;
import nz.co.kehrbusch.pentaho.connections.manage.MS365ConnectionManager;
import nz.co.kehrbusch.pentaho.util.file.GraphOutputStream;
import org.pentaho.di.core.exception.KettleException;
import org.pentaho.di.core.vfs.KettleVFS;
import org.pentaho.di.trans.Trans;
import org.pentaho.di.trans.TransMeta;
import org.pentaho.di.trans.step.StepDataInterface;
import org.pentaho.di.trans.step.StepMeta;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.nio.file.InvalidPathException;

public class MS365TextFileOutput extends MS365BaseTextFileOutput {
    private MS365ConnectionManager ms365ConnectionManager;
    private ISharepointConnection iSharepointConnection;

    public MS365TextFileOutput(StepMeta stepMeta, StepDataInterface stepDataInterface, int copyNr, TransMeta transMeta, Trans trans) {
        super(stepMeta, stepDataInterface, copyNr, transMeta, trans);
    }

    @Override
    public void initFileStreamWriter(String filename) throws KettleException {
        this.data.writer = null;

        try {
            MS365TextFileOutputData.FileStream fileStreams = null;

            try {
                if (this.data.splitEvery > 0) {
                    if (filename.equals(this.data.getFileStreamsCollection().getLastFileName())) {
                        fileStreams = this.data.getFileStreamsCollection().getLastStream();
                    }
                } else {
                    fileStreams = this.data.getFileStreamsCollection().getStream(filename);
                }

                boolean writingToFileForFirstTime = fileStreams == null;
                if (writingToFileForFirstTime) {
                    this.ms365ConnectionManager = MS365ConnectionManager.getInstance(getLogChannel());
                    this.ms365ConnectionManager.initConnections(getRepository());
                    this.iSharepointConnection = ((GraphConnectionDetails) ms365ConnectionManager.provideDetailsByConnectionName(this.meta.getConnectionName())).getISharepointConnection();

                    boolean appendToExistingFile = this.meta.isFileAppended();
                    boolean createIfNotExists = this.meta.isCreateParentFolder();

                    int maxOpenFiles = this.getMaxOpenFiles();
                    if (maxOpenFiles > 0 && this.data.getFileStreamsCollection().getNumOpenFiles() >= maxOpenFiles) {
                        this.data.getFileStreamsCollection().closeOldestOpenFile(true);
                    }

                    ISharepointFile iSharepointFile = this.getFileFromSharepoint(filename, createIfNotExists);

                    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                    BufferedOutputStream bufferedOutputStream = new GraphOutputStream(this.iSharepointConnection, outputStream, iSharepointFile, appendToExistingFile);
                    fileStreams = new MS365TextFileOutputData.FileStream(bufferedOutputStream);
                    this.data.getFileStreamsCollection().add(filename, fileStreams);
                    if (this.log.isDetailed()) {
                        this.logDetailed("Opened new file with name [" + KettleVFS.getFriendlyURI(filename) + "]");
                    }
                } else if (fileStreams.getBufferedOutputStream() == null) {
                    int maxOpenFiles = this.getMaxOpenFiles();
                    if (maxOpenFiles > 0 && this.data.getFileStreamsCollection().getNumOpenFiles() >= maxOpenFiles) {
                        this.data.getFileStreamsCollection().closeOldestOpenFile(false);
                    }
                    ISharepointFile iSharepointFile = ((GraphOutputStream) this.data.getFileStreamsCollection().getStream(filename).getBufferedOutputStream()).getiSharepointFile();
                    boolean appendToExistingFile = this.meta.isFileAppended();
                    boolean createIfNotExists = this.meta.isCreateParentFolder();
                    if (iSharepointFile == null){
                        iSharepointFile = getFileFromSharepoint(filename, createIfNotExists);
                    }

                    ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                    BufferedOutputStream bufferedOutputStream = new GraphOutputStream(this.iSharepointConnection, byteArrayOutputStream, iSharepointFile, appendToExistingFile);
                    fileStreams.setBufferedOutputStream(bufferedOutputStream);
                }
            } catch (Exception var11) {
                throw new KettleException("Error opening new file : " + var11.toString());

            }

            fileStreams.setDirty(true);
            this.data.writer = fileStreams.getBufferedOutputStream();
        } catch (KettleException var12) {
            throw var12;
        } catch (Exception var13) {
            throw new KettleException("Error opening new file : " + var13.toString());
        }
    }

    private ISharepointFile getFileFromSharepoint(String filename, boolean createIfNotExists){
        ISharepointFile iSharepointFile = null;
        try {
            iSharepointFile = this.iSharepointConnection.inflateTreeByPath(filename, createIfNotExists);
        } catch (InvalidPathException e){
            logError("No matching path found");
            e.printStackTrace();
        }
        return iSharepointFile;
    }
}
