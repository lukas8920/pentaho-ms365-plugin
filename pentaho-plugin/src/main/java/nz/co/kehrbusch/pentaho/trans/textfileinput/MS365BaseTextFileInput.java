package nz.co.kehrbusch.pentaho.trans.textfileinput;

import nz.co.kehrbusch.ms365.interfaces.entities.IStreamProvider;
import nz.co.kehrbusch.pentaho.util.file.SharepointFileWrapper;
import nz.co.kehrbusch.pentaho.util.ms365opensavedialog.providers.MS365File;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.vfs2.*;
import org.pentaho.di.core.Result;
import org.pentaho.di.core.ResultFile;
import org.pentaho.di.core.RowSet;
import org.pentaho.di.core.exception.KettleException;
import org.pentaho.di.core.row.RowDataUtil;
import org.pentaho.di.core.row.RowMeta;
import org.pentaho.di.core.row.RowMetaInterface;
import org.pentaho.di.core.util.Utils;
import org.pentaho.di.i18n.BaseMessages;
import org.pentaho.di.trans.Trans;
import org.pentaho.di.trans.TransMeta;
import org.pentaho.di.trans.step.BaseStep;
import org.pentaho.di.trans.step.StepDataInterface;
import org.pentaho.di.trans.step.StepMeta;
import org.pentaho.di.trans.step.StepMetaInterface;
import org.pentaho.di.trans.step.errorhandling.CompositeFileErrorHandler;
import org.pentaho.di.trans.step.errorhandling.FileErrorHandler;
import org.pentaho.di.trans.step.errorhandling.FileErrorHandlerContentLineNumber;
import org.pentaho.di.trans.step.errorhandling.FileErrorHandlerMissingFiles;
import org.pentaho.di.trans.steps.file.*;

import java.util.*;

public abstract class MS365BaseTextFileInput extends BaseStep implements IBaseFileInputStepControl {
    private static Class<?> PKG = org.pentaho.di.trans.steps.file.BaseFileInputStep.class;
    protected MS365TextFileInputMeta meta;
    protected MS365TextFileInputData data;

    protected abstract boolean init();

    protected abstract IBaseFileInputReader createReader(MS365TextFileInputMeta var1, MS365TextFileInputData var2, IStreamProvider var3) throws Exception;

    protected abstract SharepointFileWrapper<MS365TextFileInputMeta> getSharepointFileWrapper();

    public MS365BaseTextFileInput(StepMeta stepMeta, StepDataInterface stepDataInterface, int copyNr, TransMeta transMeta, Trans trans) {
        super(stepMeta, stepDataInterface, copyNr, transMeta, trans);
    }


    public boolean init(StepMetaInterface smi, StepDataInterface sdi) {
        this.meta = (MS365TextFileInputMeta) smi;
        this.data = (MS365TextFileInputData) sdi;

        if (!super.init(smi, sdi)) {
            return false;
        } else {
            if (this.getTransMeta().getNamedClusterEmbedManager() != null) {
                this.getTransMeta().getNamedClusterEmbedManager().passEmbeddedMetastoreKey(this, this.getTransMeta().getEmbeddedMetastoreProviderKey());
            }

            this.initErrorHandling();
            this.meta.additionalOutputFields.normalize();
            this.data.files = getSharepointFileWrapper();
            this.data.currentFileIndex = 0;
            String clusterSize = this.getVariable("Internal.Cluster.Size");
            if (!Utils.isEmpty(clusterSize) && Integer.valueOf(clusterSize) > 1) {
                String nr = this.getVariable("Internal.Slave.Transformation.Number");
                if (this.log.isDetailed()) {
                    this.logDetailed("Running on slave server #" + nr + " : assuming that each slave reads a dedicated part of the same file(s).");
                }
            }
            return this.init();
        }
    }


    protected boolean openNextFile() {
        try {
            if (this.data.currentFileIndex >= this.data.files.getStreamProviders().size()) {
                return false;
            }

            this.data.file = (MS365File) this.data.files.getStreamProviders().get(this.data.currentFileIndex);
            this.data.filename = this.data.file.getName();
            this.fillFileAdditionalFields(this.data, this.data.file);
            if (this.meta.inputFiles.passingThruFields) {
                StringBuilder sb = new StringBuilder();
                sb.append(this.data.currentFileIndex).append("_").append(this.data.file);
                this.data.currentPassThruFieldsRow = (Object[])this.data.passThruFields.get(sb.toString());
            }

            /*if (this.meta.inputFiles.isaddresult) {
                ResultFile resultFile = new ResultFile(0, this.data.file, this.getTransMeta().getName(), this.toString());
                resultFile.setComment("File was read by an Text File input step");
                this.addResultFile(resultFile);
            }*/

            if (this.log.isBasic()) {
                this.logBasic("Opening file: " + this.data.file.getName());
            }

            //this.data.dataErrorLineHandler.handleFile(this.data.file);
            this.data.reader = this.createReader(this.meta, this.data, this.data.file);
        } catch (Exception var2) {
            if (!this.handleOpenFileException(var2)) {
                return false;
            }
            this.data.reader = null;
        }

        ++this.data.currentFileIndex;
        return true;
    }

    protected boolean handleOpenFileException(Exception e) {
        String errorMsg = "Couldn't open file #" + this.data.currentFileIndex + " : " + this.data.file.getName();
        if (!this.failAfterBadFile(errorMsg)) {
            return true;
        } else {
            this.stopAll();
            this.setErrors(this.getErrors() + 1L);
            this.logError(errorMsg, e);
            return false;
        }
    }

    public boolean processRow(StepMetaInterface smi, StepDataInterface sdi) throws KettleException {
        this.meta = (MS365TextFileInputMeta) smi;
        this.data = (MS365TextFileInputData) sdi;

        if (this.first) {
            this.first = false;
            this.prepareToRowProcessing();


            if (!this.openNextFile()) {
                this.setOutputDone();
                this.closeLastFile();
                return false;
            }
        }

        do {
            if (this.data.reader != null && this.data.reader.readRow()) {
                return true;
            }

            this.closeLastFile();
        } while(this.openNextFile());
        this.setOutputDone();
        this.closeLastFile();
        return false;
    }

    protected void prepareToRowProcessing() throws KettleException {
        this.data.outputRowMeta = new RowMeta();
        RowMetaInterface[] infoStep = null;
        if (this.meta.inputFiles.acceptingFilenames) {
            infoStep = this.filesFromPreviousStep();
        }

        this.meta.getFields(this.data.outputRowMeta, this.getStepname(), infoStep, (StepMeta)null, this, this.repository, this.metaStore);
        this.data.convertRowMeta = this.data.outputRowMeta.cloneToType(2);
        //BaseFileInputStepUtils.handleMissingFiles(this.data.files, this.log, this.meta.errorHandling.errorIgnored, this.data.dataErrorLineHandler);

        for(int i = 0; i < this.meta.inputFields.length; ++i) {
            if (this.meta.inputFields[i].isRepeated()) {
                ++this.data.nr_repeats;
            }
        }
    }

    public boolean checkFeedback(long lines) {
        return super.checkFeedback(lines);
    }

    private void initErrorHandling() {
        List<FileErrorHandler> dataErrorLineHandlers = new ArrayList(2);
        if (this.meta.errorHandling.lineNumberFilesDestinationDirectory != null) {
            dataErrorLineHandlers.add(new FileErrorHandlerContentLineNumber(this.getTrans().getCurrentDate(), this.environmentSubstitute(this.meta.errorHandling.lineNumberFilesDestinationDirectory), this.meta.errorHandling.lineNumberFilesExtension, this.meta.getEncoding(), this));
        }

        if (this.meta.errorHandling.errorFilesDestinationDirectory != null) {
            dataErrorLineHandlers.add(new FileErrorHandlerMissingFiles(this.getTrans().getCurrentDate(), this.environmentSubstitute(this.meta.errorHandling.errorFilesDestinationDirectory), this.meta.errorHandling.errorFilesExtension, this.meta.getEncoding(), this));
        }

        this.data.dataErrorLineHandler = new CompositeFileErrorHandler(dataErrorLineHandlers);
    }

    private RowMetaInterface[] filesFromPreviousStep() throws KettleException {
        RowMetaInterface[] infoStep = null;
        this.data.files.getStreamProviders().clear();
        int idx = -1;
        RowSet rowSet = this.findInputRowSet(this.meta.inputFiles.acceptingStepName);

        for(Object[] fileRow = this.getRowFrom(rowSet); fileRow != null; fileRow = this.getRowFrom(rowSet)) {
            RowMetaInterface prevInfoFields = rowSet.getRowMeta();
            if (idx < 0) {
                if (this.meta.inputFiles.passingThruFields) {
                    this.data.passThruFields = new HashMap();
                    infoStep = new RowMetaInterface[]{prevInfoFields};
                    this.data.nrPassThruFields = prevInfoFields.size();
                }

                idx = prevInfoFields.indexOfValue(this.meta.inputFiles.acceptingField);
                if (idx < 0) {
                    this.logError(BaseMessages.getString(PKG, "BaseFileInputStep.Log.Error.UnableToFindFilenameField", new String[]{this.meta.inputFiles.acceptingField}));
                    this.setErrors(this.getErrors() + 1L);
                    this.stopAll();
                    return null;
                }
            }

            String fileValue = prevInfoFields.getString(fileRow, idx);

            try {
                logBasic("add to streamprovider");
                getSharepointFileWrapper().addIStreamProvider(fileValue);
            } catch (Exception var13) {
                this.logError(BaseMessages.getString(PKG, "BaseFileInputStep.Log.Error.UnableToCreateFileObject", new String[]{fileValue}), var13);
                throw new KettleException(var13);
            }
        }

        if (this.data.files.getStreamProviders().size() == 0) {
            if (this.log.isDetailed()) {
                this.logDetailed(BaseMessages.getString(PKG, "BaseFileInputStep.Log.Error.NoFilesSpecified", new String[0]));
            }

            return null;
        } else {
            return infoStep;
        }
    }

    protected void closeLastFile() {
        if (this.data.reader != null) {
            try {
                this.data.reader.close();
            } catch (Exception var3) {
                this.failAfterBadFile("Error close reader");
            }

            this.data.reader = null;
        }

        if (this.data.file != null) {
            try {
                this.data.file.disposeInputStream();
            } catch (Exception var2) {
                this.failAfterBadFile("Error close file");
            }

            this.data.file = null;
        }
    }

    public void dispose(StepMetaInterface smi, StepDataInterface sdi) {
        this.closeLastFile();
        super.dispose(smi, sdi);
    }

    public boolean failAfterBadFile(String errorMsg) {
        if (this.getStepMeta().isDoingErrorHandling() && this.data.filename != null && !this.data.rejectedFiles.containsKey(this.data.filename)) {
            this.data.rejectedFiles.put(this.data.filename, true);
            this.rejectCurrentFile(errorMsg);
        }

        return !this.meta.errorHandling.errorIgnored || !this.meta.errorHandling.skipBadFiles;
    }

    private void rejectCurrentFile(String errorMsg) {
        if (StringUtils.isNotBlank(this.meta.errorHandling.fileErrorField) || StringUtils.isNotBlank(this.meta.errorHandling.fileErrorMessageField)) {
            RowMetaInterface rowMeta = this.getInputRowMeta();
            if (rowMeta == null) {
                rowMeta = new RowMeta();
            }

            int errorFileIndex = StringUtils.isBlank(this.meta.errorHandling.fileErrorField) ? -1 : BaseFileInputStepUtils.addValueMeta(this.getStepname(), (RowMetaInterface)rowMeta, this.environmentSubstitute(this.meta.errorHandling.fileErrorField));
            int errorMessageIndex = StringUtils.isBlank(this.meta.errorHandling.fileErrorMessageField) ? -1 : BaseFileInputStepUtils.addValueMeta(this.getStepname(), (RowMetaInterface)rowMeta, this.environmentSubstitute(this.meta.errorHandling.fileErrorMessageField));

            try {
                Object[] rowData = this.getRow();
                if (rowData == null) {
                    rowData = RowDataUtil.allocateRowData(((RowMetaInterface)rowMeta).size());
                }

                if (errorFileIndex >= 0) {
                    rowData[errorFileIndex] = this.data.filename;
                }

                if (errorMessageIndex >= 0) {
                    rowData[errorMessageIndex] = errorMsg;
                }

                this.putError((RowMetaInterface)rowMeta, rowData, this.getErrors(), this.data.filename, (String)null, "ERROR_CODE");
            } catch (Exception var6) {
                this.logError("Error sending error row", var6);
            }
        }
    }

    protected void fillFileAdditionalFields(MS365TextFileInputData data, MS365File file) throws FileSystemException {
        data.shortFilename = file.getName();
        data.path = file.getPath();
        data.extension = file.getExtension();
    }
}
