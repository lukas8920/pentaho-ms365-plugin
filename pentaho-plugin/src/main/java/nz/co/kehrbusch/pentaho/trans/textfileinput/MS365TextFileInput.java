package nz.co.kehrbusch.pentaho.trans.textfileinput;

import nz.co.kehrbusch.ms365.interfaces.entities.IStreamProvider;
import nz.co.kehrbusch.pentaho.connections.manage.GraphConnectionDetails;
import nz.co.kehrbusch.pentaho.connections.manage.MS365ConnectionManager;
import nz.co.kehrbusch.pentaho.trans.textfileinput.replay.MS365FilePlayListReplay;
import nz.co.kehrbusch.pentaho.trans.textfileinput.utils.MS365TextFileInputReader;
import nz.co.kehrbusch.pentaho.util.file.SharepointFileWrapper;
import org.pentaho.di.i18n.BaseMessages;
import org.pentaho.di.trans.Trans;
import org.pentaho.di.trans.TransMeta;
import org.pentaho.di.trans.step.StepDataInterface;
import org.pentaho.di.trans.step.StepMeta;
import org.pentaho.di.trans.steps.file.IBaseFileInputReader;
import org.pentaho.di.trans.steps.fileinput.text.TextFileFilterProcessor;
import org.pentaho.di.trans.steps.fileinput.text.TextFileInputMeta;
import org.pentaho.di.ui.spoon.Spoon;

public class MS365TextFileInput extends MS365BaseTextFileInput {
    private static final Class<?> PKG = MS365TextFileInput.class;

    private GraphConnectionDetails graphConnectionDetails;

    public MS365TextFileInput(StepMeta stepMeta, StepDataInterface stepDataInterface, int copyNr, TransMeta transMeta, Trans trans) {
        super(stepMeta, stepDataInterface, copyNr, transMeta, trans);
    }

    @Override
    protected boolean init() {
        this.data.filePlayList = new MS365FilePlayListReplay(this.graphConnectionDetails.getISharepointConnection(), this.meta.content.encoding);
        this.data.filterProcessor = new TextFileFilterProcessor(this.meta.getFilter(), this);
        this.data.fileFormatType = (this.meta.getFileFormatTypeNr());
        this.data.fileType = (this.meta.getFileTypeNr());
        this.data.separator = this.environmentSubstitute(this.meta.content.separator);
        this.data.enclosure = this.environmentSubstitute(this.meta.content.enclosure);
        this.data.escapeCharacter = this.environmentSubstitute(this.meta.content.escapeCharacter);
        if (!this.meta.content.fileType.equalsIgnoreCase("CSV") || this.meta.content.separator != null && !((TextFileInputMeta)this.meta).content.separator.isEmpty()) {
            return true;
        } else {
            this.logError(BaseMessages.getString(PKG, "TextFileInput.Exception.NoSeparator", new String[0]));
            return false;
        }
    }

    @Override
    protected IBaseFileInputReader createReader(MS365TextFileInputMeta var1, MS365TextFileInputData var2, IStreamProvider var3) throws Exception {
        return new MS365TextFileInputReader(this.graphConnectionDetails.getISharepointConnection(), this, var1, var2, var3, this.log);
    }

    @Override
    protected SharepointFileWrapper<MS365TextFileInputMeta> getSharepointFileWrapper() {
        MS365ConnectionManager connectionManager = MS365ConnectionManager.getInstance(this.getLogChannel());
        if (Spoon.getInstance() == null){
            connectionManager.initConnections(this.repository);
        } else {
            connectionManager.initConnections();
        }

        this.graphConnectionDetails = (GraphConnectionDetails) connectionManager.provideDetailsByConnectionName(this.meta.getConnectionName());
        return new SharepointFileWrapper<>(this.meta, graphConnectionDetails.getISharepointConnection(), this.getLogChannel());
    }
}
