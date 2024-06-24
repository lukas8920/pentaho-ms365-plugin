package nz.co.kehrbusch.pentaho.trans.textfileinput.replay;

import nz.co.kehrbusch.ms365.interfaces.ISharepointConnection;
import nz.co.kehrbusch.ms365.interfaces.entities.IStreamProvider;
import nz.co.kehrbusch.pentaho.util.ms365opensavedialog.providers.MS365File;
import org.pentaho.di.core.exception.KettleException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashSet;
import java.util.Set;

public class MS365FilePlayListReplayLineNumberFile extends MS365FilePlayListReplayFile {
    Set<Long> lineNumbers = new HashSet();

    public MS365FilePlayListReplayLineNumberFile(ISharepointConnection iSharepointConnection, MS365File lineNumberFile, String encoding, MS365File processingFile, String filePart) throws KettleException {
        super(processingFile, filePart);
        this.initialize(iSharepointConnection, lineNumberFile, encoding);
    }

    private void initialize(ISharepointConnection iSharepointConnection, MS365File lineNumberFile, String encoding) throws KettleException {
        BufferedReader reader = null;

        try {
            if (encoding == null || encoding.isEmpty()) {
                System.out.println("Econding is null");
                reader = new BufferedReader(new InputStreamReader(lineNumberFile.getInputStream(iSharepointConnection)));
            } else {
                System.out.println("Encoding is " + encoding);
                reader = new BufferedReader(new InputStreamReader(lineNumberFile.getInputStream(iSharepointConnection), encoding));
            }

            String line = null;
            long counter = 0L;

            while((line = reader.readLine()) != null) {

                if (line.length() > 0) {
                    this.lineNumbers.add(counter);
                    counter += 1;
                }
            }
        } catch (Exception var12) {
            throw new KettleException("Could not read line number file " + lineNumberFile.getName(), var12);
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException var11) {
                    throw new KettleException("Could not close line number file " + lineNumberFile.getName(), var11);
                }
            }
        }
    }

    @Override
    public boolean isProcessingNeeded(IStreamProvider file, long lineNr, String filePart) throws KettleException {
        return this.lineNumbers.contains(lineNr);
    }
}
