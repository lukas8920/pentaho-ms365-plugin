package nz.co.kehrbusch.pentaho.trans.getfilenames;

import nz.co.kehrbusch.ms365.interfaces.ISharepointConnection;
import nz.co.kehrbusch.pentaho.connections.manage.GraphConnectionDetails;
import nz.co.kehrbusch.pentaho.connections.manage.MS365ConnectionManager;
import nz.co.kehrbusch.pentaho.util.file.SharepointFileWrapper;
import nz.co.kehrbusch.pentaho.util.ms365opensavedialog.providers.MS365File;
import org.pentaho.di.core.Const;
import org.pentaho.di.core.exception.KettleException;
import org.pentaho.di.core.exception.KettleStepException;
import org.pentaho.di.core.row.RowDataUtil;
import org.pentaho.di.core.row.RowMeta;
import org.pentaho.di.core.row.RowMetaInterface;
import org.pentaho.di.core.util.Utils;
import org.pentaho.di.i18n.BaseMessages;
import org.pentaho.di.trans.Trans;
import org.pentaho.di.trans.TransMeta;
import org.pentaho.di.trans.step.StepDataInterface;
import org.pentaho.di.trans.step.StepMeta;
import org.pentaho.di.trans.step.StepMetaInterface;
import org.pentaho.di.trans.steps.getfilenames.GetFileNames;
import org.pentaho.di.ui.spoon.Spoon;

public class MS365GetFileNames extends GetFileNames {
    private static final Class<?> PKG = MS365GetFileNames.class;

    private MS365GetFileNamesMeta meta;
    private MS365GetFileNamesData data;

    public MS365GetFileNames(StepMeta stepMeta, StepDataInterface stepDataInterface, int copyNr, TransMeta transMeta, Trans trans) {
        super(stepMeta, stepDataInterface, copyNr, transMeta, trans);
    }

    private Object[] buildEmptyRow() {
        Object[] rowData = RowDataUtil.allocateRowData(this.data.outputRowMeta.size());
        return rowData;
    }

    public boolean processRow(StepMetaInterface smi, StepDataInterface sdi) throws KettleException {
        if (!this.meta.isFileField()) {
            if (this.data.filenr >= this.data.filessize) {
                this.setOutputDone();
                return false;
            }
        } else {
            if (this.data.filenr >= this.data.filessize) {
                this.data.readrow = this.getRow();
            }

            if (this.data.readrow == null) {
                this.setOutputDone();
                return false;
            }

            if (this.first) {
                this.first = false;
                this.data.inputRowMeta = this.getInputRowMeta();
                this.data.outputRowMeta = this.data.inputRowMeta.clone();
                this.meta.getFields(this.data.outputRowMeta, this.getStepname(), (RowMetaInterface[])null, (StepMeta)null, this, this.repository, this.metaStore);
                this.data.totalpreviousfields = this.data.inputRowMeta.size();
                if (Utils.isEmpty(this.meta.getDynamicFilenameField())) {
                    this.logError(BaseMessages.getString(PKG, "GetFileNames.Log.NoField", new String[0]));
                    throw new KettleException(BaseMessages.getString(PKG, "GetFileNames.Log.NoField", new String[0]));
                }

                if (this.data.indexOfFilenameField < 0) {
                    this.data.indexOfFilenameField = this.data.inputRowMeta.indexOfValue(this.meta.getDynamicFilenameField());
                    if (this.data.indexOfFilenameField < 0) {
                        this.logError(BaseMessages.getString(PKG, "GetFileNames.Log.ErrorFindingField", new String[]{this.meta.getDynamicFilenameField()}));
                        throw new KettleException(BaseMessages.getString(PKG, "GetFileNames.Exception.CouldnotFindField", new String[]{this.meta.getDynamicFilenameField()}));
                    }
                }

                if (!Utils.isEmpty(this.meta.getDynamicWildcardField()) && this.data.indexOfWildcardField < 0) {
                    this.data.indexOfWildcardField = this.data.inputRowMeta.indexOfValue(this.meta.getDynamicWildcardField());
                    if (this.data.indexOfWildcardField < 0) {
                        this.logError(BaseMessages.getString(PKG, "GetFileNames.Log.ErrorFindingField", new String[0]) + "[" + this.meta.getDynamicWildcardField() + "]");
                        throw new KettleException(BaseMessages.getString(PKG, "GetFileNames.Exception.CouldnotFindField", new String[]{this.meta.getDynamicWildcardField()}));
                    }
                }

                if (!Utils.isEmpty(this.meta.getDynamicExcludeWildcardField()) && this.data.indexOfExcludeWildcardField < 0) {
                    this.data.indexOfExcludeWildcardField = this.data.inputRowMeta.indexOfValue(this.meta.getDynamicExcludeWildcardField());
                    if (this.data.indexOfExcludeWildcardField < 0) {
                        this.logError(BaseMessages.getString(PKG, "GetFileNames.Log.ErrorFindingField", new String[0]) + "[" + this.meta.getDynamicExcludeWildcardField() + "]");
                        throw new KettleException(BaseMessages.getString(PKG, "GetFileNames.Exception.CouldnotFindField", new String[]{this.meta.getDynamicExcludeWildcardField()}));
                    }
                }
            }
        }

        try {
            Object[] outputRow = this.buildEmptyRow();
            int outputIndex = 0;
            Object[] extraData = new Object[this.data.nrStepFields];
            if (this.meta.isFileField()) {
                if (this.data.filenr >= this.data.filessize) {
                    String filename = this.getInputRowMeta().getString(this.data.readrow, this.data.indexOfFilenameField);
                    String wildcard = "";
                    if (this.data.indexOfWildcardField >= 0) {
                        wildcard = this.getInputRowMeta().getString(this.data.readrow, this.data.indexOfWildcardField);
                    }

                    String excludewildcard = "";
                    if (this.data.indexOfExcludeWildcardField >= 0) {
                        excludewildcard = this.getInputRowMeta().getString(this.data.readrow, this.data.indexOfExcludeWildcardField);
                    }

                    String[] filesname = new String[]{filename};
                    String[] filesmask = new String[]{wildcard};
                    String[] excludefilesmask = new String[]{excludewildcard};

                    if (this.data.files == null){
                        this.data.files = this.getFileSharepointFileWrapper();
                    }
                    this.data.files.addIStreamProvider(filesname, filesmask, excludefilesmask);

                    this.data.filessize = this.data.files.getStreamProviders().size();
                    this.data.filenr = 0;
                }

                outputRow = (Object[])this.data.readrow.clone();
            }

            if (this.data.filessize > 0) {
                this.data.file = (MS365File) this.data.files.getStreamProviders().get(this.data.filenr);

                int var16 = outputIndex + 1;
                extraData[outputIndex] = this.data.file.getName();
                extraData[var16++] = this.data.file.getPath() + this.data.file.getName();

                //extraData[var16++] = //KettleVFS.getFilename(this.data.file.getParent());
                extraData[var16++] = this.data.file.getType().toString();
                if (this.data.file.getCreatedDate() != null){
                    extraData[var16++] = this.data.file.getCreatedDate();
                }
                if (this.data.file.getLastModifiedDate() != null){
                    extraData[var16++] = this.data.file.getLastModifiedDate();
                }
                extraData[var16++] = this.data.file.getWebUrl();
                extraData[var16++] = this.data.file.getExtension();
                extraData[var16++] = new Long(this.data.file.getSize());

                if (this.meta.includeRowNumber() && !Utils.isEmpty(this.meta.getRowNumberField())) {
                    extraData[var16++] = new Long(this.data.rownr);
                }

                ++this.data.rownr;
                outputRow = RowDataUtil.addRowData(outputRow, this.data.totalpreviousfields, extraData);
                this.putRow(this.data.outputRowMeta, outputRow);
                if (this.meta.getRowLimit() > 0L && this.data.rownr >= this.meta.getRowLimit()) {
                    this.setOutputDone();
                    return false;
                }
            }
        } catch (Exception var15) {
            throw new KettleStepException(var15);
        }

        ++this.data.filenr;
        if (this.checkFeedback(this.getLinesInput()) && this.log.isBasic()) {
            this.logBasic(BaseMessages.getString(PKG, "GetFileNames.Log.NrLine", new String[]{"" + this.getLinesInput()}));
        }

        return true;
    }

    public boolean init(StepMetaInterface smi, StepDataInterface sdi) {
        this.meta = (MS365GetFileNamesMeta) smi;
        this.data = (MS365GetFileNamesData) sdi;
        if (super.init(smi, sdi)) {
            if (this.getTransMeta().getNamedClusterEmbedManager() != null) {
                this.getTransMeta().getNamedClusterEmbedManager().passEmbeddedMetastoreKey(this, this.getTransMeta().getEmbeddedMetastoreProviderKey());
            }

            try {
                this.data.outputRowMeta = new RowMeta();
                this.meta.getFields(this.data.outputRowMeta, this.getStepname(), (RowMetaInterface[])null, (StepMeta)null, this, this.repository, this.metaStore);
                this.data.nrStepFields = this.data.outputRowMeta.size();
                if (!this.meta.isFileField()) {
                    if (this.data.files == null){
                        this.data.files = this.getFileSharepointFileWrapper();
                    }
                    this.data.filessize = this.data.files.getStreamProviders().size();
                    //this.handleMissingFiles();
                } else {
                    this.data.filessize = 0;
                }
            } catch (Exception var4) {
                this.logError("Error initializing step: " + var4.toString());
                this.logError(Const.getStackTracker(var4));
                return false;
            }

            this.data.rownr = 1L;
            this.data.filenr = 0;
            this.data.totalpreviousfields = 0;
            return true;
        } else {
            return false;
        }
    }

    public SharepointFileWrapper<MS365GetFileNamesMeta> getFileSharepointFileWrapper(){
        MS365ConnectionManager connectionManager = MS365ConnectionManager.getInstance(this.getLogChannel());
        if (Spoon.getInstance() == null){
            connectionManager.initConnections(this.repository);
        } else {
            connectionManager.initConnections();
        }
        log.logBasic("Connection Name: " + this.meta.getConnectionName());
        ISharepointConnection iSharepointConnection = ((GraphConnectionDetails) connectionManager.provideDetailsByConnectionName(this.meta.getConnectionName())).getISharepointConnection();
        return this.meta.getFileWrapper(iSharepointConnection, this.getLogChannel());
    }

    public void dispose(StepMetaInterface smi, StepDataInterface sdi) {
        this.meta = (MS365GetFileNamesMeta) smi;
        this.data = (MS365GetFileNamesData) sdi;
        if (this.data.file != null) {
            try {
                this.data.file.disposeInputStream();
                this.data.file = null;
            } catch (Exception var4) {
            }
        }

        super.dispose(smi, sdi);
    }
}
