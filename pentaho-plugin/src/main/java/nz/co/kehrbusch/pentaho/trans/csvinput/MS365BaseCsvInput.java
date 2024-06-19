package nz.co.kehrbusch.pentaho.trans.csvinput;

import nz.co.kehrbusch.pentaho.util.file.PositionableByteInputStream;
import org.apache.commons.io.ByteOrderMark;
import org.apache.commons.io.input.BOMInputStream;
import org.apache.commons.lang.StringUtils;
import org.pentaho.di.core.exception.KettleConversionException;
import org.pentaho.di.core.exception.KettleException;
import org.pentaho.di.core.exception.KettleFileException;
import org.pentaho.di.core.exception.KettleValueException;
import org.pentaho.di.core.row.RowDataUtil;
import org.pentaho.di.core.row.RowMeta;
import org.pentaho.di.core.row.RowMetaInterface;
import org.pentaho.di.core.row.ValueMetaInterface;
import org.pentaho.di.core.util.Utils;
import org.pentaho.di.i18n.BaseMessages;
import org.pentaho.di.trans.Trans;
import org.pentaho.di.trans.TransMeta;
import org.pentaho.di.trans.TransPreviewFactory;
import org.pentaho.di.trans.step.StepDataInterface;
import org.pentaho.di.trans.step.StepMeta;
import org.pentaho.di.trans.step.StepMetaInterface;
import org.pentaho.di.trans.steps.csvinput.*;
import org.pentaho.di.trans.steps.fileinput.text.BOMDetector;
import org.pentaho.di.trans.steps.textfileinput.EncodingType;
import org.pentaho.di.trans.steps.textfileinput.TextFileInput;
import org.pentaho.di.trans.steps.textfileinput.TextFileInputField;
import org.pentaho.di.ui.core.dialog.EnterNumberDialog;
import org.pentaho.di.ui.core.dialog.EnterTextDialog;
import org.pentaho.di.ui.core.dialog.PreviewRowsDialog;
import org.pentaho.di.ui.trans.dialog.TransPreviewProgressDialog;

import java.io.*;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Logger;

//Original Pentaho class, but with protected access and injected abstract methods
public abstract class MS365BaseCsvInput extends CsvInput {
    private static final Logger logger = Logger.getLogger(MS365BaseCsvInput.class.getName());

    private static Class<?> PKG = CsvInput.class;
    private MS365CsvInputMeta meta;
    private MS365CsvInputData data;

    public MS365BaseCsvInput(StepMeta stepMeta, StepDataInterface stepDataInterface, int copyNr, TransMeta transMeta, Trans trans) {
        super(stepMeta, stepDataInterface, copyNr, transMeta, trans);
    }

    public abstract void initConnection(MS365CsvInputMeta ms365CsvInputMeta);

    public abstract InputStream getInputStream(String filename);

    @Override
    public boolean processRow(StepMetaInterface smi, StepDataInterface sdi) throws KettleException {
        this.meta = (MS365CsvInputMeta) smi;
        this.data = (MS365CsvInputData) sdi;

        if (this.first) {
            this.first = false;

            this.initConnection(this.meta);

            this.data.outputRowMeta = new RowMeta();
            this.meta.getFields(this.data.outputRowMeta, this.getStepname(), (RowMetaInterface[])null, (StepMeta)null, this, this.repository, this.metaStore);
            if (this.data.filenames == null) {
                this.getFilenamesFromPreviousSteps();
            }

            this.data.parallel = this.meta.isRunningInParallel() && this.data.totalNumberOfSteps > 1;
            this.data.convertRowMeta = this.data.outputRowMeta.clone();
            Iterator var3 = this.data.convertRowMeta.getValueMetaList().iterator();

            while(var3.hasNext()) {
                ValueMetaInterface valueMeta = (ValueMetaInterface)var3.next();
                valueMeta.setStorageType(1);
            }

            this.data.filenameFieldIndex = -1;
            if (!Utils.isEmpty(this.meta.getFilenameField()) && this.meta.isIncludingFilename()) {
                this.data.filenameFieldIndex = this.meta.getInputFields().length;
            }

            this.data.rownumFieldIndex = -1;
            if (!Utils.isEmpty(this.meta.getRowNumField())) {
                this.data.rownumFieldIndex = this.meta.getInputFields().length;
                if (this.data.filenameFieldIndex >= 0) {
                    ++this.data.rownumFieldIndex;
                }
            }

            if (this.data.parallel) {
                this.prepareToRunInParallel();
            }

            if (!this.openNextFile()) {
                this.setOutputDone();
                return false;
            }
        }

        if (this.data.parallel && this.data.totalBytesRead >= this.data.blockToRead) {
            this.setOutputDone();
            return false;
        } else {
            try {
                Object[] outputRowData = this.readOneRow(false, false);
                if (outputRowData == null) {
                    if (this.openNextFile()) {
                        return true;
                    }

                    this.setOutputDone();
                    return false;
                }

                this.putRow(this.data.outputRowMeta, outputRowData);
                if (this.checkFeedback(this.getLinesInput()) && this.log.isBasic()) {
                    this.logBasic(BaseMessages.getString(PKG, "CsvInput.Log.LineNumber", new String[]{Long.toString(this.getLinesInput())}));
                }
            } catch (KettleConversionException var7) {
                KettleConversionException e = var7;
                if (!this.getStepMeta().isDoingErrorHandling()) {
                    throw new KettleException(var7.getMessage(), (Throwable)var7.getCauses().get(0));
                }

                StringBuilder errorDescriptions = new StringBuilder(100);
                StringBuilder errorFields = new StringBuilder(50);

                for(int i = 0; i < e.getCauses().size(); ++i) {
                    if (i > 0) {
                        errorDescriptions.append(", ");
                        errorFields.append(", ");
                    }

                    errorDescriptions.append(((Exception)e.getCauses().get(i)).getMessage());
                    errorFields.append(((ValueMetaInterface)e.getFields().get(i)).toStringMeta());
                }

                this.putError(this.data.outputRowMeta, e.getRowData(), (long)e.getCauses().size(), errorDescriptions.toString(), errorFields.toString(), "CSVINPUT001");
            }

            return true;
        }
    }

    private void prepareToRunInParallel() throws KettleException {
        try {
            String[] var1 = this.data.filenames;
            int var2 = var1.length;

            int i;
            for(i = 0; i < var2; ++i) {
                String filename = var1[i];
                //todo fetch size
                long size = 21;
                this.data.fileSizes.add(size);
                CsvInputData var10000 = this.data;
                var10000.totalFileSize += size;
            }

            this.data.blockToRead = Math.round((double)this.data.totalFileSize / (double)this.data.totalNumberOfSteps);
            this.data.startPosition = this.data.blockToRead * (long)this.data.stepNumber;
            this.data.endPosition = this.data.startPosition + this.data.blockToRead;
            long totalFileSize = 0L;

            for(i = 0; i < this.data.fileSizes.size(); ++i) {
                long size = (Long)this.data.fileSizes.get(i);
                if (this.data.startPosition >= totalFileSize && this.data.startPosition < totalFileSize + size) {
                    this.data.filenr = i;
                    this.data.startFilenr = i;
                    if (this.data.startPosition == 0L) {
                        this.data.bytesToSkipInFirstFile = 0L;
                    } else {
                        this.data.bytesToSkipInFirstFile = this.data.startPosition - totalFileSize;
                    }
                    break;
                }

                totalFileSize += size;
            }

            if (this.data.filenames.length > 0) {
                this.logBasic(BaseMessages.getString(PKG, "CsvInput.Log.ParallelFileNrAndPositionFeedback", new String[]{this.data.filenames[this.data.filenr], Long.toString((Long)this.data.fileSizes.get(this.data.filenr)), Long.toString(this.data.bytesToSkipInFirstFile), Long.toString(this.data.blockToRead)}));
            }

        } catch (Exception var7) {
            throw new KettleException(BaseMessages.getString(PKG, "CsvInput.Exception.ErrorPreparingParallelRun", new String[0]), var7);
        }
    }

    private void getFilenamesFromPreviousSteps() throws KettleException {
        List<String> filenames = new ArrayList();
        boolean firstRow = true;
        int index = -1;

        for(Object[] row = this.getRow(); row != null; row = this.getRow()) {
            String filenameField;
            if (firstRow) {
                firstRow = false;
                filenameField = this.environmentSubstitute(this.meta.getFilenameField());
                index = this.getInputRowMeta().indexOfValue(filenameField);
                if (index < 0) {
                    throw new KettleException(BaseMessages.getString(PKG, "CsvInput.Exception.FilenameFieldNotFound", new String[]{filenameField}));
                }
            }

            filenameField = this.getInputRowMeta().getString(row, index);
            filenames.add(filenameField);
        }

        this.data.filenames = (String[])filenames.toArray(new String[filenames.size()]);
        Arrays.stream(this.data.filenames).forEach(filename -> logger.info("Initialized filename: " + filename));
        this.logBasic(BaseMessages.getString(PKG, "CsvInput.Log.ReadingFromNrFiles", new String[]{Integer.toString(this.data.filenames.length)}));
    }



    @Override
    public void dispose(StepMetaInterface smi, StepDataInterface sdi ) {

        // Casting to step-specific implementation classes is safe
        MS365CsvInputMeta meta = (MS365CsvInputMeta) smi;
        MS365CsvInputData data = (MS365CsvInputData) sdi;

        try {
            if (this.data.fc != null) {
                this.data.fc.close();
            }
        } catch (Exception var5) {
            this.logError("Error closing file channel", var5);
        }

        try {
            if (this.data.fis != null) {
                this.data.fis.close();
            }
        } catch (Exception var4) {
            this.logError("Error closing file input stream", var4);
        }
        // Add any step-specific initialization that may be needed here

        // Call superclass dispose()
        super.dispose( meta, data );
    }

    private boolean openNextFile() throws KettleException {
        try {
            this.data.closeFile();
            if (this.data.filenr >= this.data.filenames.length) {
                return false;
            } else {
                this.data.fieldsMapping = this.createFieldMapping(this.data.filenames[this.data.filenr], this.meta);

                //Inject Third party Code
                /*FileObject fileObject = KettleVFS.getFileObject(this.data.filenames[this.data.filenr], this.getTransMeta());
                if (!(fileObject instanceof LocalFile)) {
                    throw new KettleException(BaseMessages.getString(PKG, "CsvInput.Log.OnlyLocalFilesAreSupported", new String[0]));
                } else {*/
                    if (this.meta.isLazyConversionActive()) {
                        this.data.binaryFilename = this.data.filenames[this.data.filenr].getBytes();
                    }

                    //String vfsFilename = KettleVFS.getFilename(fileObject);
                    String filename = this.data.filenames[this.data.filenr];
                    logger.info("Get size of BOM");
                    int bomSize = this.getBOMSize(filename);
                    logger.info("Size of BOM: " + bomSize);
                    //this.data.fis = new FileInputStream(vfsFilename);
                    this.data.fis = getInputStream(filename);

                    if (0 != bomSize) {
                        this.data.fis.skip((long)bomSize);
                    }

                    this.data.fc = new PositionableByteInputStream(this.data.fis);
                    this.data.bb = ByteBuffer.allocateDirect(this.data.preferredBufferSize);
                    if (this.data.parallel && this.data.bytesToSkipInFirstFile > 0L) {
                        this.data.fc.position(this.data.bytesToSkipInFirstFile);
                        if (this.needToSkipRow()) {
                            while(!this.data.newLineFound()) {
                                this.data.moveEndBufferPointer();
                            }

                            this.data.moveEndBufferPointer();
                            if (this.data.newLineFound()) {
                                this.data.moveEndBufferPointer();
                            }
                        }

                        this.data.setStartBuffer(this.data.getEndBuffer());
                    }

                    //cannot be implemented for the moment
                    /*if (this.meta.isAddResultFile()) {
                        ResultFile resultFile = new ResultFile(0, fileObject, this.getTransMeta().getName(), this.toString());
                        resultFile.setComment("File was read by a Csv input step");
                        this.addResultFile(resultFile);
                    }*/

                    ++this.data.filenr;
                    if (this.meta.isHeaderPresent() && (!this.data.parallel || this.data.bytesToSkipInFirstFile <= 0L)) {
                        this.readOneRow(true, false);
                        this.logBasic(BaseMessages.getString(PKG, "CsvInput.Log.HeaderRowSkipped", new String[]{this.data.filenames[this.data.filenr - 1]}));
                        if (this.data.fieldsMapping.size() == 0) {
                            return false;
                        }
                    }

                    this.data.rowNumber = 1L;
                    this.data.bytesToSkipInFirstFile = -1L;
                    return true;
                }
            //}
        } catch (KettleException var5) {
            throw var5;
        } catch (Exception var6) {
            throw new KettleException(var6);
        }
    }

    protected int getBOMSize(String vfsFilename) throws Exception {
        int bomSize = 0;
        InputStream fis = this.getInputStream(vfsFilename);

        try {
            BufferedInputStream bis = new BufferedInputStream(fis);

            try {
                BOMDetector bom = new BOMDetector(bis);
                if (bom.bomExist()) {
                    bomSize = bom.getBomSize();
                }
            } catch (Throwable var9) {
                try {
                    bis.close();
                } catch (Throwable var8) {
                    var9.addSuppressed(var8);
                }

                throw var9;
            }

            bis.close();
        } catch (Throwable var10) {
            try {
                fis.close();
            } catch (Throwable var7) {
                var10.addSuppressed(var7);
            }

            throw var10;
        }

        fis.close();
        return bomSize;
    }

    FieldsMapping createFieldMapping(String fileName, CsvInputMeta csvInputMeta) throws KettleException {
        FieldsMapping mapping = null;
        if (csvInputMeta.isHeaderPresent()) {
            String[] fieldNames = this.readFieldNamesFromFile(fileName, csvInputMeta);
            mapping = NamedFieldsMapping.mapping(fieldNames, fieldNames(csvInputMeta));
        } else {
            int fieldsCount = csvInputMeta.getInputFields() == null ? 0 : csvInputMeta.getInputFields().length;
            mapping = UnnamedFieldsMapping.mapping(fieldsCount);
        }

        return (FieldsMapping)mapping;
    }

    String[] readFieldNamesFromFile(String fileName, CsvInputMeta csvInputMeta) throws KettleException {
        String delimiter = this.environmentSubstitute(csvInputMeta.getDelimiter());
        String enclosure = this.environmentSubstitute(csvInputMeta.getEnclosure());
        String realEncoding = this.environmentSubstitute(csvInputMeta.getEncoding());

        try {
            String[] var12;
            try {
                InputStream originalInputStream = this.getInputStream(fileName);
                BOMInputStream inputStream = new BOMInputStream(originalInputStream, new ByteOrderMark[]{ByteOrderMark.UTF_8, ByteOrderMark.UTF_16LE, ByteOrderMark.UTF_16BE});

                try {
                    InputStreamReader reader = null;
                    if (Utils.isEmpty(realEncoding)) {
                        reader = new InputStreamReader(inputStream);
                    } else {
                        reader = new InputStreamReader(inputStream, realEncoding);
                    }

                    EncodingType encodingType = EncodingType.guessEncodingType(reader.getEncoding());
                    String line = TextFileInput.getLine(this.log, reader, encodingType, 1, new StringBuilder(1000));
                    String[] fieldNames = guessStringsFromLine(this.log, line, delimiter, enclosure, csvInputMeta.getEscapeCharacter());
                    if (!Utils.isEmpty(csvInputMeta.getEnclosure())) {
                        removeEnclosure(fieldNames, csvInputMeta.getEnclosure());
                    }

                    trimFieldNames(fieldNames);
                    var12 = fieldNames;
                } catch (Throwable var15) {
                    try {
                        inputStream.close();
                    } catch (Throwable var14) {
                        var15.addSuppressed(var14);
                    }

                    throw var15;
                }

                inputStream.close();
            } catch (Throwable var16) {
                throw var16;
            }

            return var12;
        } catch (IOException var17) {
            throw new KettleFileException(BaseMessages.getString(PKG, "CsvInput.Exception.CreateFieldMappingError", new String[0]), var17);
        }
    }

    static void trimFieldNames(String[] strings) {
        if (strings != null) {
            for(int i = 0; i < strings.length; ++i) {
                strings[i] = strings[i].trim();
            }
        }

    }

    static String[] fieldNames(CsvInputMeta csvInputMeta) {
        TextFileInputField[] fields = csvInputMeta.getInputFields();
        String[] fieldNames = new String[fields.length];

        for(int i = 0; i < fields.length; ++i) {
            fieldNames[i] = EncodingType.removeBOMIfPresent(fields[i].getName());
        }

        return fieldNames;
    }

    static void removeEnclosure(String[] fields, String enclosure) {
        if (fields != null) {
            for(int i = 0; i < fields.length; ++i) {
                if (fields[i].startsWith(enclosure) && fields[i].endsWith(enclosure) && fields[i].length() > 1) {
                    fields[i] = fields[i].substring(1, fields[i].length() - 1);
                }
            }
        }

    }

    private boolean needToSkipRow() {
        try {
            this.data.fc.position(this.data.fc.position() - 1L);
            this.data.resizeBufferIfNeeded();
            if (this.data.newLineFound()) {
                this.data.moveEndBufferPointer(false);
                boolean var1 = this.data.newLineFound();
                return var1;
            }

            this.data.moveEndBufferPointer(false);
        } catch (IOException var12) {
            var12.printStackTrace();
        } finally {
            try {
                this.data.fc.position(this.data.fc.position() + 1L);
            } catch (IOException var11) {
            }

        }

        return true;
    }

    private Object[] readOneRow(boolean skipRow, boolean ignoreEnclosures) throws KettleException {
        try {
            Object[] outputRowData = RowDataUtil.allocateRowData(this.data.outputRowMeta.size());
            int outputIndex = 0;
            boolean newLineFound = false;
            boolean endOfBuffer = false;
            List<Exception> conversionExceptions = null;
            List<ValueMetaInterface> exceptionFields = null;
            if (StringUtils.isBlank(this.meta.getFileFormat())) {
                this.meta.setFileFormat("mixed");
            }

            for(; !newLineFound && outputIndex < this.data.fieldsMapping.size(); this.data.setStartBuffer(this.data.getEndBuffer())) {
                if (this.data.resizeBufferIfNeeded()) {
                    if (outputRowData != null && outputIndex > 0) {
                        if (this.meta.isIncludingFilename() && !Utils.isEmpty(this.meta.getFilenameField())) {
                            if (this.meta.isLazyConversionActive()) {
                                outputRowData[this.data.filenameFieldIndex] = this.data.binaryFilename;
                            } else {
                                outputRowData[this.data.filenameFieldIndex] = this.data.filenames[this.data.filenr - 1];
                            }
                        }

                        if (this.data.isAddingRowNumber) {
                            outputRowData[this.data.rownumFieldIndex] = Long.valueOf((long)(this.data.rowNumber++));
                        }

                        this.incrementLinesInput();
                        return outputRowData;
                    }

                    return null;
                }

                boolean delimiterFound = false;
                boolean enclosureFound = false;
                boolean doubleLineEnd = false;
                int escapedEnclosureFound = 0;
                boolean ignoreEnclosuresInField = ignoreEnclosures;

                int fieldFirstBytePosition;
                while(!delimiterFound && !newLineFound && !endOfBuffer) {
                    if (this.data.delimiterFound()) {
                        delimiterFound = true;
                    } else {
                        int enclosurePosition;
                        if ((!this.meta.isNewlinePossibleInFields() || outputIndex == this.data.fieldsMapping.size() - 1) && this.data.newLineFound()) {
                            newLineFound = true;

                            for(enclosurePosition = 0; enclosurePosition < this.data.encodingType.getLength(); ++enclosurePosition) {
                                this.data.moveEndBufferPointer();
                            }

                            if (this.data.newLineFound()) {
                                doubleLineEnd = true;
                            }
                        } else if (this.data.enclosureFound() && !ignoreEnclosuresInField) {
                            enclosurePosition = this.data.getEndBuffer();
                            fieldFirstBytePosition = this.data.getStartBuffer();
                            if (fieldFirstBytePosition != enclosurePosition) {
                                ignoreEnclosuresInField = true;
                            } else {
                                enclosureFound = true;

                                boolean keepGoing;
                                do {
                                    if (this.data.moveEndBufferPointer()) {
                                        enclosureFound = false;
                                        break;
                                    }

                                    keepGoing = !this.data.enclosureFound();
                                    if (!keepGoing) {
                                        if (!this.data.endOfBuffer() && this.data.moveEndBufferPointer()) {
                                            break;
                                        }

                                        if (this.data.enclosure.length > 1) {
                                            this.data.moveEndBufferPointer();
                                        }

                                        keepGoing = this.data.enclosureFound();
                                        if (keepGoing) {
                                            ++escapedEnclosureFound;
                                        }
                                    }
                                } while(keepGoing);

                                if (this.data.endOfBuffer()) {
                                    endOfBuffer = true;
                                    break;
                                }
                            }
                        } else if (this.data.moveEndBufferPointer()) {
                            endOfBuffer = true;
                            break;
                        }
                    }
                }

                byte[] field = this.data.getField(delimiterFound, enclosureFound, newLineFound, endOfBuffer);
                if (escapedEnclosureFound > 0) {
                    if (this.log.isRowLevel()) {
                        this.logRowlevel("Escaped enclosures found in " + new String(field));
                    }

                    field = this.data.removeEscapedEnclosures(field, escapedEnclosureFound);
                }

                fieldFirstBytePosition = outputIndex++;
                int actualFieldIndex = this.data.fieldsMapping.fieldMetaIndex(fieldFirstBytePosition);
                if (actualFieldIndex != -1) {
                    if (!skipRow) {
                        if (this.meta.isLazyConversionActive()) {
                            outputRowData[actualFieldIndex] = field;
                        } else {
                            ValueMetaInterface sourceValueMeta = this.data.convertRowMeta.getValueMeta(actualFieldIndex);

                            try {
                                outputRowData[actualFieldIndex] = sourceValueMeta.convertBinaryStringToNativeType(field);
                            } catch (KettleValueException var19) {
                                outputRowData[actualFieldIndex] = null;
                                if (conversionExceptions == null) {
                                    conversionExceptions = new ArrayList();
                                    exceptionFields = new ArrayList();
                                }

                                conversionExceptions.add(var19);
                                exceptionFields.add(sourceValueMeta);
                            }
                        }
                    } else {
                        outputRowData[actualFieldIndex] = null;
                    }
                }

                if (!newLineFound && outputIndex < this.data.fieldsMapping.size() || newLineFound && doubleLineEnd) {
                    for(int i = 0; !this.data.newLineFound() && i < this.data.delimiter.length; ++i) {
                        this.data.moveEndBufferPointer();
                    }

                    switch(this.meta.getFileFormatTypeNr()) {
                        case 0:
                            if (this.data.newLineFound()) {
                                if (doubleLineEnd) {
                                    this.data.moveEndBufferPointerXTimes(this.data.encodingType.getLength());
                                } else {
                                    this.data.moveEndBufferPointerXTimes(this.data.encodingType.getLength());
                                    if (!this.data.newLineFound()) {
                                        throw new KettleFileException(BaseMessages.getString(PKG, "TextFileInput.Log.SingleLineFound", new String[0]));
                                    }
                                }
                            }
                            break;
                        case 2:
                            if (this.data.isCarriageReturn() || doubleLineEnd) {
                                this.data.moveEndBufferPointerXTimes(this.data.encodingType.getLength());
                            }
                    }
                }
            }

            if (!newLineFound && !this.data.resizeBufferIfNeeded()) {
                do {
                    this.data.moveEndBufferPointer();
                } while(!this.data.resizeBufferIfNeeded() && !this.data.newLineFound());

                if (!this.data.resizeBufferIfNeeded()) {
                    while(this.data.newLineFound()) {
                        this.data.moveEndBufferPointer();
                        if (this.data.resizeBufferIfNeeded()) {
                            break;
                        }
                    }
                }

                this.data.setStartBuffer(this.data.getEndBuffer());
            }

            if (this.meta.isIncludingFilename() && !Utils.isEmpty(this.meta.getFilenameField())) {
                if (this.meta.isLazyConversionActive()) {
                    outputRowData[this.data.filenameFieldIndex] = this.data.binaryFilename;
                } else {
                    outputRowData[this.data.filenameFieldIndex] = this.data.filenames[this.data.filenr - 1];
                }
            }

            if (this.data.isAddingRowNumber) {
                outputRowData[this.data.rownumFieldIndex] = Long.valueOf((long)(this.data.rowNumber++));
            }

            if (!ignoreEnclosures) {
                this.incrementLinesInput();
            }

            if (conversionExceptions != null && conversionExceptions.size() > 0) {
                throw new KettleConversionException("There were " + conversionExceptions.size() + " conversion errors on line " + this.getLinesInput(), conversionExceptions, exceptionFields, outputRowData);
            } else {
                return outputRowData;
            }
        } catch (KettleConversionException var20) {
            throw var20;
        } catch (IOException var21) {
            throw new KettleFileException("Exception reading line using NIO", var21);
        }
    }

    public boolean init(StepMetaInterface smi, StepDataInterface sdi) {
        this.meta = (MS365CsvInputMeta) smi;
        this.data = (MS365CsvInputData) sdi;
        if (super.init(smi, sdi)) {
            String realEncoding = this.environmentSubstitute(this.meta.getEncoding());
            this.data.preferredBufferSize = Integer.parseInt(this.environmentSubstitute(this.meta.getBufferSize()));
            if (this.getTransMeta().findNrPrevSteps(this.getStepMeta()) == 0) {
                String filename = this.filenameValidatorForInputFiles(this.meta.getFilename());
                if (Utils.isEmpty(filename)) {
                    this.logError(BaseMessages.getString(PKG, "CsvInput.MissingFilename.Message", new String[0]));
                    return false;
                }

                this.data.filenames = new String[]{filename};
                Arrays.stream(this.data.filenames).forEach(file -> logger.info("After init: " + file));
            } else {
                this.data.filenames = null;
                this.data.filenr = 0;
            }

            this.data.totalBytesRead = 0L;
            this.data.encodingType = EncodingType.guessEncodingType(realEncoding);

            try {
                this.data.delimiter = this.data.encodingType.getBytes(this.environmentSubstitute(this.meta.getDelimiter()), realEncoding);
                if (Utils.isEmpty(this.meta.getEnclosure())) {
                    this.data.enclosure = null;
                } else {
                    this.data.enclosure = this.data.encodingType.getBytes(this.environmentSubstitute(this.meta.getEnclosure()), realEncoding);
                }
            } catch (UnsupportedEncodingException var5) {
                this.logError(BaseMessages.getString(PKG, "CsvInput.BadEncoding.Message", new String[0]), var5);
                return false;
            }

            this.data.isAddingRowNumber = !Utils.isEmpty(this.meta.getRowNumField());
            this.data.stopReading = false;
            if (this.meta.isRunningInParallel()) {
                this.data.stepNumber = this.getUniqueStepNrAcrossSlaves();
                this.data.totalNumberOfSteps = this.getUniqueStepCountAcrossSlaves();
                this.data.fileSizes = new ArrayList();
                this.data.totalFileSize = 0L;
            }

            if (this.data.delimiter.length == 1) {
                this.data.delimiterMatcher = new SingleBytePatternMatcher();
            } else {
                this.data.delimiterMatcher = new MultiBytePatternMatcher();
            }

            if (this.data.enclosure == null) {
                this.data.enclosureMatcher = new EmptyPatternMatcher();
            } else if (this.data.enclosure.length == 1) {
                this.data.enclosureMatcher = new SingleBytePatternMatcher();
            } else {
                this.data.enclosureMatcher = new MultiBytePatternMatcher();
            }

            switch(this.data.encodingType) {
                case DOUBLE_BIG_ENDIAN:
                    this.data.crLfMatcher = new MultiByteBigCrLfMatcher();
                    break;
                case DOUBLE_LITTLE_ENDIAN:
                    this.data.crLfMatcher = new MultiByteLittleCrLfMatcher();
                    break;
                default:
                    this.data.crLfMatcher = new SingleByteCrLfMatcher();
            }

            return true;
        } else {
            return false;
        }
    }

    String filenameValidatorForInputFiles(String filename) {
        return filename.startsWith("/sharepoint") && filename.endsWith(".csv") ? filename : "";
    }
}
