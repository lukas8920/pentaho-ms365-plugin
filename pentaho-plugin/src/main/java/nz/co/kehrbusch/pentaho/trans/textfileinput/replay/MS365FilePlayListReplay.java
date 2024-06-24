package nz.co.kehrbusch.pentaho.trans.textfileinput.replay;

import nz.co.kehrbusch.ms365.interfaces.ISharepointConnection;
import nz.co.kehrbusch.pentaho.util.ms365opensavedialog.providers.MS365File;
import org.pentaho.di.core.exception.KettleException;

public class MS365FilePlayListReplay {
    private final String encoding;
    private MS365FilePlayListReplayFile currentLineNumberFile;
    private final ISharepointConnection iSharepointConnection;

    public MS365FilePlayListReplay(ISharepointConnection iSharepointConnection, String encoding) {
        this.encoding = encoding;
        this.iSharepointConnection = iSharepointConnection;
    }

    private MS365File getCurrentProcessingFile() {
        MS365File result = null;
        if (this.currentLineNumberFile != null) {
            result = (MS365File) this.currentLineNumberFile.getProcessingFile();
        }

        return result;
    }

    private String getCurrentProcessingFilePart() {
        String result = null;
        if (this.currentLineNumberFile != null) {
            result = this.currentLineNumberFile.getProcessingFilePart();
        }

        return result;
    }

    public boolean isProcessingNeeded(MS365File file, long lineNr, String filePart) throws KettleException {
        this.initializeCurrentIfNeeded(file, filePart);
        return this.currentLineNumberFile.isProcessingNeeded(file, lineNr, filePart);
    }

    private void initializeCurrentIfNeeded(MS365File file, String filePart) throws KettleException {
        if (!file.equals(this.getCurrentProcessingFile()) || !filePart.equals(this.getCurrentProcessingFilePart())) {
            this.initializeCurrent(file, filePart);
        }

    }

    private void initializeCurrent(MS365File file, String filePart) throws KettleException {
        this.currentLineNumberFile = new MS365FilePlayListReplayLineNumberFile(iSharepointConnection, file, this.encoding, file, filePart);
    }
}
