package nz.co.kehrbusch.pentaho.trans.textfileinput.replay;

import nz.co.kehrbusch.ms365.interfaces.entities.IStreamProvider;
import org.pentaho.di.core.exception.KettleException;

public class MS365FilePlayListReplayFile {
    private IStreamProvider processingFile;
    private String processingFilePart;

    public MS365FilePlayListReplayFile(IStreamProvider processingFile, String processingFilePart) {
        this.processingFile = processingFile;
        this.processingFilePart = processingFilePart;
    }

    IStreamProvider getProcessingFile() {
        return this.processingFile;
    }

    String getProcessingFilePart() {
        return this.processingFilePart;
    }

    public boolean isProcessingNeeded(IStreamProvider file, long lineNr, String filePart) throws KettleException {
        return false;
    }
}
