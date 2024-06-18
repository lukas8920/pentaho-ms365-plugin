package nz.co.kehrbusch.pentaho.trans.csvinput;

import nz.co.kehrbusch.ms365.interfaces.entities.IStreamProvider;
import nz.co.kehrbusch.pentaho.connections.manage.GraphConnectionDetails;
import nz.co.kehrbusch.pentaho.connections.manage.MS365ConnectionManager;
import nz.co.kehrbusch.pentaho.util.file.SharepointFileWrapper;
import org.pentaho.di.trans.Trans;
import org.pentaho.di.trans.TransMeta;
import org.pentaho.di.trans.step.StepDataInterface;
import org.pentaho.di.trans.step.StepMeta;
import org.pentaho.di.trans.step.StepMetaInterface;

import java.io.*;
import java.nio.file.InvalidPathException;
import java.util.logging.Logger;

public class MS365CsvInput extends MS365BaseCsvInput {
    private static final Logger log = Logger.getLogger(MS365CsvInput.class.getName());

    private MS365CsvInputMeta ms365CsvInputMeta;
    private SharepointFileWrapper sharepointFileWrapper;
    private GraphConnectionDetails graphConnectionDetails;

    public MS365CsvInput(StepMeta stepMeta, StepDataInterface stepDataInterface, int copyNr, TransMeta transMeta, Trans trans) {
        super(stepMeta, stepDataInterface, copyNr, transMeta, trans);
    }

    @Override
    public void initConnection(MS365CsvInputMeta ms365CsvInputMeta){
        this.ms365CsvInputMeta = ms365CsvInputMeta;
        MS365ConnectionManager connectionManager = MS365ConnectionManager.getInstance();
        this.graphConnectionDetails = (GraphConnectionDetails) connectionManager.provideDetailsByConnectionName(this.ms365CsvInputMeta.getConnectionName());
        this.sharepointFileWrapper = new SharepointFileWrapper(this.ms365CsvInputMeta, graphConnectionDetails.getISharepointConnection());
    }

    @Override
    public InputStream getInputStream(String filename){
        log.info("Get input stream for: " + filename);
        IStreamProvider iStreamProvider;
        InputStream inputStream = new ByteArrayInputStream(new byte[0]);;

        try {
            iStreamProvider = this.sharepointFileWrapper.getIStreamProviderByPath(filename);
        } catch (InvalidPathException e){
            logError("No file can be found on the sharepoint with this filename.");
            return new BufferedInputStream(inputStream);
        } catch (NullPointerException e){
            logError("No MS365 connection has been set up in the step.");
            return new BufferedInputStream(inputStream);
        }

        try {
            inputStream = iStreamProvider.getInputStream(this.graphConnectionDetails.getISharepointConnection());
        } catch (IOException e){
            logError("No input stream could be received from the server.");
            return new BufferedInputStream(inputStream);
        }

        return inputStream;
    }

    @Override
    public void dispose(StepMetaInterface smi, StepDataInterface sdi) {
        if (this.sharepointFileWrapper != null){
            this.sharepointFileWrapper.getStreamProviders().forEach(provider -> {
                try {
                    provider.disposeInputStream();
                } catch (IOException e){
                    logError("Disposal of Input Stream was not possible.");
                }
            });
        }

        super.dispose(smi, sdi);
    }
}
