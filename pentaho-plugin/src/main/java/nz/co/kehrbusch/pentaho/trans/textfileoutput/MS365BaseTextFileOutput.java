package nz.co.kehrbusch.pentaho.trans.textfileoutput;

import org.apache.commons.vfs2.FileObject;
import org.pentaho.di.core.Const;
import org.pentaho.di.core.ResultFile;
import org.pentaho.di.core.WriterOutputStream;
import org.pentaho.di.core.compress.CompressionOutputStream;
import org.pentaho.di.core.compress.CompressionProvider;
import org.pentaho.di.core.compress.CompressionProviderFactory;
import org.pentaho.di.core.compress.zip.ZIPCompressionProvider;
import org.pentaho.di.core.exception.KettleException;
import org.pentaho.di.core.exception.KettleFileException;
import org.pentaho.di.core.exception.KettleStepException;
import org.pentaho.di.core.exception.KettleValueException;
import org.pentaho.di.core.fileinput.CharsetToolkit;
import org.pentaho.di.core.row.RowMetaInterface;
import org.pentaho.di.core.row.ValueMetaInterface;
import org.pentaho.di.core.util.Utils;
import org.pentaho.di.core.variables.VariableSpace;
import org.pentaho.di.core.vfs.KettleVFS;
import org.pentaho.di.i18n.BaseMessages;
import org.pentaho.di.trans.Trans;
import org.pentaho.di.trans.TransMeta;
import org.pentaho.di.trans.step.*;
import org.pentaho.di.trans.steps.textfileoutput.TextFileField;
import org.pentaho.di.trans.steps.textfileoutput.TextFileOutputData;
import org.pentaho.di.trans.steps.textfileoutput.TextFileOutputMeta;

import java.io.*;
import java.util.*;

public abstract class MS365BaseTextFileOutput extends BaseStep implements StepInterface {
        private static Class<?> PKG = TextFileOutputMeta.class;
        private static final boolean COMPATIBILITY_APPEND_NO_HEADER;
        public MS365TextFileOutputMeta meta;
        public MS365TextFileOutputData data;

        public MS365BaseTextFileOutput(StepMeta stepMeta, StepDataInterface stepDataInterface, int copyNr, TransMeta transMeta, Trans trans) {
            super(stepMeta, stepDataInterface, copyNr, transMeta, trans);
        }

        private void initFieldNumbers(RowMetaInterface outputRowMeta, TextFileField[] outputFields) throws KettleException {
            this.data.fieldnrs = new int[outputFields.length];

            for(int i = 0; i < outputFields.length; ++i) {
                this.data.fieldnrs[i] = outputRowMeta.indexOfValue(outputFields[i].getName());
                if (this.data.fieldnrs[i] < 0) {
                    throw new KettleStepException("Field [" + outputFields[i].getName() + "] couldn't be found in the input stream!");
                }
            }

        }

        public boolean isFileExists(String filename) throws KettleException {
            try {
                return this.getFileObject(filename, this.getTransMeta()).exists();
            } catch (Exception var3) {
                throw new KettleException("Error opening new file : " + var3.toString());
            }
        }

        private void initServletStreamWriter() throws KettleException {
            this.data.writer = null;

            try {
                Writer writer = this.getTrans().getServletPrintWriter();
                if (Utils.isEmpty(this.meta.getEncoding())) {
                    this.data.writer = new WriterOutputStream(writer);
                } else {
                    this.data.writer = new WriterOutputStream(writer, this.meta.getEncoding());
                }

            } catch (Exception var2) {
                throw new KettleException("Error opening new file : " + var2.toString());
            }
        }

        public abstract void initFileStreamWriter(String filename) throws KettleException;

        public String getOutputFileName(Object[] row) throws KettleException {
            String filename = null;
            if (row == null) {
                if (this.data.writer != null) {
                    filename = this.data.getFileStreamsCollection().getLastFileName();
                } else {
                    filename = this.meta.getFileName();
                    if (filename == null) {
                        throw new KettleFileException(BaseMessages.getString(PKG, "TextFileOutput.Exception.FileNameNotSet", new String[0]));
                    }

                    filename = this.buildFilename(this.environmentSubstitute(filename), true);
                }
            } else {
                this.data.fileNameFieldIndex = this.getInputRowMeta().indexOfValue(this.meta.getFileNameField());
                if (this.data.fileNameFieldIndex < 0) {
                    throw new KettleStepException(BaseMessages.getString(PKG, "TextFileOutput.Exception.FileNameFieldNotFound", new String[]{this.meta.getFileNameField()}));
                }

                this.data.fileNameMeta = this.getInputRowMeta().getValueMeta(this.data.fileNameFieldIndex);
                this.data.fileName = this.data.fileNameMeta.getString(row[this.data.fileNameFieldIndex]);
                if (this.data.fileName == null) {
                    throw new KettleFileException(BaseMessages.getString(PKG, "TextFileOutput.Exception.FileNameNotSet", new String[0]));
                }

                filename = this.buildFilename(this.environmentSubstitute(this.data.fileName), true);
            }

            return filename;
        }

        public int getFlushInterval() {
            String flushIntervalStr = this.getTransMeta().getVariable("KETTLE_FILE_OUTPUT_MAX_STREAM_LIFE");
            int flushInterval = 0;
            if (flushIntervalStr != null) {
                try {
                    flushInterval = Integer.parseInt(flushIntervalStr);
                } catch (Exception var4) {
                }
            }

            return flushInterval;
        }

        public int getMaxOpenFiles() {
            String maxStreamCountStr = this.getTransMeta().getVariable("KETTLE_FILE_OUTPUT_MAX_STREAM_COUNT");
            int maxStreamCount = 0;
            if (maxStreamCountStr != null) {
                try {
                    maxStreamCount = Integer.parseInt(maxStreamCountStr);
                } catch (Exception var4) {
                }
            }

            return maxStreamCount;
        }

        private boolean writeRowToServlet(Object[] row) throws KettleException {
            if (row != null) {
                if (this.data.writer == null) {
                    this.initServletStreamWriter();
                }

                this.first = false;
                this.writeRow(this.data.outputRowMeta, row);
                this.putRow(this.data.outputRowMeta, row);
                if (this.checkFeedback(this.getLinesOutput())) {
                    this.logBasic("linenr " + this.getLinesOutput());
                }

                return true;
            } else {
                if (this.data.writer == null && !Utils.isEmpty(this.environmentSubstitute(this.meta.getEndedLine()))) {
                    this.initServletStreamWriter();
                    this.initBinaryDataFields();
                }

                this.writeEndedLine();
                this.setOutputDone();
                return false;
            }
        }

        public boolean isWriteHeader(String filename) throws KettleException {
            boolean writingToFileForFirstTime = this.first;
            boolean isWriteHeader = this.meta.isHeaderEnabled();
            if (isWriteHeader) {
                if (this.data.splitEvery > 0) {
                    writingToFileForFirstTime |= !filename.equals(this.data.getFileStreamsCollection().getLastFileName());
                } else {
                    writingToFileForFirstTime |= this.data.getFileStreamsCollection().getStream(filename) == null;
                }
            }

            isWriteHeader &= writingToFileForFirstTime && (!this.meta.isFileAppended() || !COMPATIBILITY_APPEND_NO_HEADER && !this.isFileExists(filename));
            return isWriteHeader;
        }

        private boolean writeRowToFile(Object[] row) throws KettleException {
            String filename;
            if (row != null) {
                filename = this.getOutputFileName(this.meta.isFileNameInField() ? row : null);
                boolean isWriteHeader = this.isWriteHeader(filename);
                if (this.data.writer == null || this.meta.isFileNameInField()) {
                    this.initFileStreamWriter(filename);
                }

                this.first = false;
                if (isWriteHeader) {
                    this.writeHeader();
                }

                if (!this.meta.isFileNameInField() && this.getLinesOutput() > 0L && this.data.splitEvery > 0 && (this.getLinesOutput() + (long)this.meta.getFooterShift()) % (long)this.data.splitEvery == 0L) {
                    if (this.meta.isFooterEnabled()) {
                        this.writeHeader();
                    }

                    this.closeFile(filename);
                    ++this.data.splitnr;
                    this.data.fos = null;
                    this.data.out = null;
                    this.data.writer = null;
                    filename = this.getOutputFileName((Object[])null);
                    isWriteHeader = this.isWriteHeader(filename);
                    this.initFileStreamWriter(filename);
                    if (isWriteHeader) {
                        this.writeHeader();
                    }
                }

                this.writeRow(this.data.outputRowMeta, row);
                this.putRow(this.data.outputRowMeta, row);
                if (this.checkFeedback(this.getLinesOutput())) {
                    this.logBasic("linenr " + this.getLinesOutput());
                }

                int flushInterval = this.getFlushInterval();
                if (flushInterval > 0) {
                    long currentTime = (new Date()).getTime();
                    if (this.data.lastFileFlushTime == 0L) {
                        this.data.lastFileFlushTime = currentTime;
                    } else if (currentTime - this.data.lastFileFlushTime > (long)flushInterval) {
                        try {
                            this.data.getFileStreamsCollection().flushOpenFiles(false);
                        } catch (IOException var8) {
                            throw new KettleException("Unable to flush open files", var8);
                        }

                        this.data.lastFileFlushTime = (new Date()).getTime();
                    }
                }

                return true;
            } else {
                if (this.data.writer != null) {
                    if (this.data.outputRowMeta != null && this.meta.isFooterEnabled()) {
                        this.writeHeader();
                    }
                } else if (!Utils.isEmpty(this.environmentSubstitute(this.meta.getEndedLine())) && !this.meta.isFileNameInField()) {
                    filename = this.getOutputFileName((Object[])null);
                    this.initFileStreamWriter(filename);
                    this.initBinaryDataFields();
                }

                if (this.data.writer != null) {
                    this.writeEndedLine();
                }

                try {
                    this.flushOpenFiles(true);
                } catch (IOException var9) {
                    throw new KettleException("Unable to flush open files", var9);
                }

                this.setOutputDone();
                return false;
            }
        }

        public void flushOpenFiles(boolean closeAfterFlush) throws IOException {
            this.data.getFileStreamsCollection().flushOpenFiles(true);
        }

        public synchronized boolean processRow(StepMetaInterface smi, StepDataInterface sdi) throws KettleException {
            this.meta = (MS365TextFileOutputMeta) smi;
            this.data = (MS365TextFileOutputData) sdi;
            if (Utils.isEmpty(this.meta.getEncoding())) {
                this.meta.setEncoding(CharsetToolkit.getDefaultSystemCharset().name());
            }

            Object[] row = this.getRow();
            if (this.first) {
                if (row != null) {
                    this.data.inputRowMeta = this.getInputRowMeta();
                    this.data.outputRowMeta = this.data.inputRowMeta.clone();
                }

                this.initBinaryDataFields();
                if (this.data.outputRowMeta != null) {
                    this.initFieldNumbers(this.data.outputRowMeta, this.meta.getOutputFields());
                    if (row != null) {
                        this.meta.getFields(this.data.outputRowMeta, this.getStepname(), (RowMetaInterface[])null, (StepMeta)null, this, this.repository, this.metaStore);
                    }

                    this.meta.calcMetaWithFieldOptions(this.data);
                }
            }

            return this.writeRowTo(row);
        }

        protected boolean writeRowTo(Object[] row) throws KettleException {
            return this.meta.isServletOutput() ? this.writeRowToServlet(row) : this.writeRowToFile(row);
        }

        public void writeRow(RowMetaInterface rowMeta, Object[] r) throws KettleStepException {
            try {
                int i;
                ValueMetaInterface v;
                Object valueData;
                if (Utils.isEmpty(this.meta.getOutputFields())) {
                    for(i = 0; i < rowMeta.size(); ++i) {
                        if (i > 0 && this.data.binarySeparator.length > 0) {
                            this.data.writer.write(this.data.binarySeparator);
                        }

                        v = rowMeta.getValueMeta(i);
                        valueData = r[i];
                        this.writeField(v, valueData, (byte[])null);
                    }
                } else {
                    for(i = 0; i < this.meta.getOutputFields().length; ++i) {
                        if (i > 0 && this.data.binarySeparator.length > 0) {
                            this.data.writer.write(this.data.binarySeparator);
                        }

                        v = this.meta.getMetaWithFieldOptions()[i];
                        valueData = r[this.data.fieldnrs[i]];
                        this.writeField(v, valueData, this.data.binaryNullValue[i]);
                    }
                }

                this.data.writer.write(this.data.binaryNewline);
                this.incrementLinesOutput();
            } catch (Exception var6) {
                throw new KettleStepException("Error writing line", var6);
            }
        }

        private byte[] formatField(ValueMetaInterface v, Object valueData) throws KettleValueException {
            if (v.isString()) {
                if (v.isStorageBinaryString() && v.getTrimType() == 0 && v.getLength() < 0 && Utils.isEmpty(v.getStringEncoding())) {
                    return (byte[])valueData;
                } else {
                    String svalue = valueData instanceof String ? (String)valueData : v.getString(valueData);
                    return this.convertStringToBinaryString(v, Const.trimToType(svalue, v.getTrimType()));
                }
            } else {
                return v.getBinaryString(valueData);
            }
        }

        private byte[] convertStringToBinaryString(ValueMetaInterface v, String string) throws KettleValueException {
            int length = v.getLength();
            if (string == null) {
                return new byte[0];
            } else if (length > -1 && length < string.length()) {
                String tmp = string.substring(0, length);
                if (Utils.isEmpty(v.getStringEncoding())) {
                    return tmp.getBytes();
                } else {
                    try {
                        return tmp.getBytes(v.getStringEncoding());
                    } catch (UnsupportedEncodingException var14) {
                        throw new KettleValueException("Unable to convert String to Binary with specified string encoding [" + v.getStringEncoding() + "]", var14);
                    }
                }
            } else {
                byte[] text;
                if (Utils.isEmpty(this.meta.getEncoding())) {
                    text = string.getBytes();
                } else {
                    try {
                        text = string.getBytes(this.meta.getEncoding());
                    } catch (UnsupportedEncodingException var16) {
                        throw new KettleValueException("Unable to convert String to Binary with specified string encoding [" + v.getStringEncoding() + "]", var16);
                    }
                }

                if (length > string.length()) {
                    int size;
                    Object var6 = null;

                    byte[] filler;
                    try {
                        if (!Utils.isEmpty(this.meta.getEncoding())) {
                            filler = " ".getBytes(this.meta.getEncoding());
                        } else {
                            filler = " ".getBytes();
                        }

                        size = text.length + filler.length * (length - string.length());
                    } catch (UnsupportedEncodingException var15) {
                        throw new KettleValueException(var15);
                    }

                    byte[] bytes = new byte[size];
                    System.arraycopy(text, 0, bytes, 0, text.length);
                    if (filler.length == 1) {
                        Arrays.fill(bytes, text.length, size, filler[0]);
                    } else {
                        int currIndex = text.length;

                        for(int i = 0; i < length - string.length(); ++i) {
                            byte[] var10 = filler;
                            int var11 = filler.length;

                            for(int var12 = 0; var12 < var11; ++var12) {
                                byte b = var10[var12];
                                bytes[currIndex++] = b;
                            }
                        }
                    }

                    return bytes;
                } else {
                    return text;
                }
            }
        }

        private byte[] getBinaryString(String string) throws KettleStepException {
            try {
                return this.data.hasEncoding ? string.getBytes(this.meta.getEncoding()) : string.getBytes();
            } catch (Exception var3) {
                throw new KettleStepException(var3);
            }
        }

        private void writeField(ValueMetaInterface v, Object valueData, byte[] nullString) throws KettleStepException {
            try {
                byte[] str;
                if (nullString != null && v.isNull(valueData)) {
                    str = nullString;
                } else if (this.meta.isFastDump()) {
                    if (valueData instanceof byte[]) {
                        str = (byte[])valueData;
                    } else {
                        str = this.getBinaryString(valueData == null ? "" : valueData.toString());
                    }
                } else {
                    str = this.formatField(v, valueData);
                }

                if (str != null && str.length > 0) {
                    List<Integer> enclosures = null;
                    boolean writeEnclosures = this.isWriteEnclosureForWriteField(str);
                    if (writeEnclosures) {
                        this.data.writer.write(this.data.binaryEnclosure);
                        enclosures = this.getEnclosurePositions(str);
                    }

                    if (enclosures == null) {
                        this.data.writer.write(str);
                    } else {
                        int from = 0;

                        for(int i = 0; i < enclosures.size(); ++i) {
                            int position = (Integer)enclosures.get(i);
                            this.data.writer.write(str, from, position + this.data.binaryEnclosure.length - from);
                            this.data.writer.write(this.data.binaryEnclosure);
                            from = position + this.data.binaryEnclosure.length;
                        }

                        if (from < str.length) {
                            this.data.writer.write(str, from, str.length - from);
                        }
                    }

                    if (writeEnclosures) {
                        this.data.writer.write(this.data.binaryEnclosure);
                    }
                }

            } catch (Exception var10) {
                throw new KettleStepException("Error writing field content to file", var10);
            }
        }

        private List<Integer> getEnclosurePositions(byte[] str) {
            List<Integer> positions = null;
            if (this.data.binaryEnclosure != null && this.data.binaryEnclosure.length > 0) {
                int i = 0;

                for(int len = str.length - this.data.binaryEnclosure.length + 1; i < len; ++i) {
                    boolean found = true;

                    for(int x = 0; found && x < this.data.binaryEnclosure.length; ++x) {
                        if (str[i + x] != this.data.binaryEnclosure[x]) {
                            found = false;
                        }
                    }

                    if (found) {
                        if (positions == null) {
                            positions = new ArrayList();
                        }

                        positions.add(i);
                    }
                }
            }

            return positions;
        }

        protected boolean writeEndedLine() {
            boolean retval = false;

            try {
                String sLine = this.environmentSubstitute(this.meta.getEndedLine());
                if (sLine != null && sLine.trim().length() > 0) {
                    this.data.writer.write(this.getBinaryString(sLine));
                    this.incrementLinesOutput();
                }
            } catch (Exception var3) {
                this.logError("Error writing ended tag line: " + var3.toString());
                this.logError(Const.getStackTracker(var3));
                retval = true;
            }

            return retval;
        }

        protected boolean writeHeader() {
            boolean retval = false;
            RowMetaInterface r = this.data.outputRowMeta;

            try {
                int i;
                if (!Utils.isEmpty(this.meta.getOutputFields())) {
                    for(i = 0; i < this.meta.getOutputFields().length; ++i) {
                        String fieldName = this.meta.getOutputFields()[i].getName();
                        ValueMetaInterface v = r.searchValueMeta(fieldName);
                        if (i > 0 && this.data.binarySeparator.length > 0) {
                            this.data.writer.write(this.data.binarySeparator);
                        }

                        boolean writeEnclosure = this.isWriteEnclosureForFieldName(v, fieldName);
                        if (writeEnclosure) {
                            this.data.writer.write(this.data.binaryEnclosure);
                        }

                        this.data.writer.write(this.getBinaryString(fieldName));
                        if (writeEnclosure) {
                            this.data.writer.write(this.data.binaryEnclosure);
                        }
                    }

                    this.data.writer.write(this.data.binaryNewline);
                } else if (r != null) {
                    if (this.data.inputRowMeta != null) {
                        r = this.data.inputRowMeta;
                    }

                    for(i = 0; i < r.size(); ++i) {
                        if (i > 0 && this.data.binarySeparator.length > 0) {
                            this.data.writer.write(this.data.binarySeparator);
                        }

                        ValueMetaInterface v = r.getValueMeta(i);
                        boolean writeEnclosure = this.isWriteEnclosureForValueMetaInterface(v);
                        if (writeEnclosure) {
                            this.data.writer.write(this.data.binaryEnclosure);
                        }

                        this.data.writer.write(this.getBinaryString(v.getName()));
                        if (writeEnclosure) {
                            this.data.writer.write(this.data.binaryEnclosure);
                        }
                    }

                    this.data.writer.write(this.data.binaryNewline);
                } else {
                    this.data.writer.write(this.getBinaryString("no rows selected" + Const.CR));
                }
            } catch (Exception var7) {
                this.logError("Error writing header line: " + var7.toString());
                this.logError(Const.getStackTracker(var7));
                retval = true;
            }

            this.incrementLinesOutput();
            return retval;
        }

        public String buildFilename(String filename, boolean ziparchive) {
            return this.meta.buildFilename(filename, this.meta.getExtension(), this, this.getCopy(), this.getPartitionID(), this.data.splitnr, ziparchive, this.meta);
        }

        protected boolean closeFile(String filename) {
            try {
                this.data.getFileStreamsCollection().closeFile(filename);
                return true;
            } catch (Exception var3) {
                this.logError("Exception trying to close file: " + var3.toString());
                this.setErrors(1L);
                return false;
            }
        }

        protected boolean closeFile() {
            boolean retval;
            try {
                if (this.data.writer != null) {
                    this.data.getFileStreamsCollection().closeStream(this.data.writer);
                }

                this.data.writer = null;
                this.data.out = null;
                this.data.fos = null;
                if (this.log.isDebug()) {
                    this.logDebug("Closing normal file ...");
                }

                retval = true;
            } catch (Exception var3) {
                this.logError("Exception trying to close file: " + var3.toString());
                this.setErrors(1L);
                this.data.writer = null;
                this.data.out = null;
                this.data.fos = null;
                retval = false;
            }

            return retval;
        }

        public boolean init(StepMetaInterface smi, StepDataInterface sdi) {
            this.meta = (MS365TextFileOutputMeta) smi;
            this.data = (MS365TextFileOutputData) sdi;
            if (this.getTransMeta().getNamedClusterEmbedManager() != null) {
                this.getTransMeta().getNamedClusterEmbedManager().passEmbeddedMetastoreKey(this.getTransMeta(), this.getTransMeta().getEmbeddedMetastoreProviderKey());
            }

            if (!super.init(smi, sdi)) {
                return false;
            } else {
                this.data.splitnr = 0;
                if (!this.meta.isDoNotOpenNewFileInit() && !this.meta.isFileNameInField()) {
                    try {
                        this.initOutput();
                    } catch (Exception var5) {
                        if (this.getParentVariableSpace() == null) {
                            this.logError("Couldn't open file " + KettleVFS.getFriendlyURI(this.meta.getFileName()) + "." + this.meta.getExtension(), var5);
                        } else {
                            this.logError("Couldn't open file " + KettleVFS.getFriendlyURI(this.getParentVariableSpace().environmentSubstitute(this.meta.getFileName())) + "." + this.getParentVariableSpace().environmentSubstitute(this.meta.getExtension()), var5);
                        }

                        this.setErrors(1L);
                        this.stopAll();
                    }
                }

                try {
                    this.initBinaryDataFields();
                } catch (Exception var4) {
                    this.logError("Couldn't initialize binary data fields", var4);
                    this.setErrors(1L);
                    this.stopAll();
                }

                return true;
            }
        }

        protected void initOutput() throws KettleException {
            if (this.meta.isServletOutput()) {
                this.initServletStreamWriter();
            } else {
                String filename = this.getOutputFileName((Object[])null);
                this.initFileStreamWriter(filename);
            }

        }

        protected void initBinaryDataFields() throws KettleException {
            try {
                this.data.hasEncoding = !Utils.isEmpty(this.meta.getEncoding());
                this.data.binarySeparator = new byte[0];
                this.data.binaryEnclosure = new byte[0];
                this.data.binaryNewline = new byte[0];
                if (this.data.hasEncoding) {
                    if (!Utils.isEmpty(this.meta.getSeparator())) {
                        this.data.binarySeparator = this.environmentSubstitute(this.meta.getSeparator()).getBytes(this.meta.getEncoding());
                    }

                    if (!Utils.isEmpty(this.meta.getEnclosure())) {
                        this.data.binaryEnclosure = this.environmentSubstitute(this.meta.getEnclosure()).getBytes(this.meta.getEncoding());
                    }

                    if (!Utils.isEmpty(this.meta.getNewline())) {
                        this.data.binaryNewline = this.meta.getNewline().getBytes(this.meta.getEncoding());
                    }
                } else {
                    if (!Utils.isEmpty(this.meta.getSeparator())) {
                        this.data.binarySeparator = this.environmentSubstitute(this.meta.getSeparator()).getBytes();
                    }

                    if (!Utils.isEmpty(this.meta.getEnclosure())) {
                        this.data.binaryEnclosure = this.environmentSubstitute(this.meta.getEnclosure()).getBytes();
                    }

                    if (!Utils.isEmpty(this.meta.getNewline())) {
                        this.data.binaryNewline = this.environmentSubstitute(this.meta.getNewline()).getBytes();
                    }
                }

                this.data.binaryNullValue = new byte[this.meta.getOutputFields().length][];

                for(int i = 0; i < this.meta.getOutputFields().length; ++i) {
                    this.data.binaryNullValue[i] = null;
                    String nullString = this.meta.getOutputFields()[i].getNullString();
                    if (!Utils.isEmpty(nullString)) {
                        if (this.data.hasEncoding) {
                            this.data.binaryNullValue[i] = nullString.getBytes(this.meta.getEncoding());
                        } else {
                            this.data.binaryNullValue[i] = nullString.getBytes();
                        }
                    }
                }

                this.data.splitEvery = this.meta.getSplitEvery(this.variables);
            } catch (Exception var3) {
                throw new KettleException("Unexpected error while encoding binary fields", var3);
            }
        }

        protected void close() throws IOException {
            if (!this.meta.isServletOutput()) {
                this.data.getFileStreamsCollection().flushOpenFiles(true);
            }

        }

        public void dispose(StepMetaInterface smi, StepDataInterface sdi) {
            this.meta = (MS365TextFileOutputMeta) smi;
            this.data = (MS365TextFileOutputData) sdi;

            try {
                this.close();
            } catch (Exception var4) {
                this.logError("Unexpected error closing file", var4);
                this.setErrors(1L);
            }

            this.data.writer = null;
            this.data.out = null;
            this.data.fos = null;
            super.dispose(smi, sdi);
        }

        public boolean containsSeparatorOrEnclosure(byte[] source, byte[] separator, byte[] enclosure) {
            boolean result = false;
            boolean enclosureExists = enclosure != null && enclosure.length > 0;
            boolean separatorExists = separator != null && separator.length > 0;
            if (separatorExists || enclosureExists) {
                for(int index = 0; !result && index < source.length; ++index) {
                    int i;
                    if (enclosureExists && source[index] == enclosure[0]) {
                        if (index + enclosure.length <= source.length) {
                            result = true;

                            for(i = 1; i < enclosure.length; ++i) {
                                if (source[index + i] != enclosure[i]) {
                                    result = false;
                                    break;
                                }
                            }
                        }
                    } else if (separatorExists && source[index] == separator[0] && index + separator.length <= source.length) {
                        result = true;

                        for(i = 1; i < separator.length; ++i) {
                            if (source[index + i] != separator[i]) {
                                result = false;
                                break;
                            }
                        }
                    }
                }
            }

            return result;
        }

        private void createParentFolder(String filename) throws Exception {
            FileObject parentfolder = null;

            try {
                parentfolder = this.getFileObject(filename, this.getTransMeta()).getParent();
                if (parentfolder.exists()) {
                    if (this.isDetailed()) {
                        this.logDetailed(BaseMessages.getString(PKG, "TextFileOutput.Log.ParentFolderExist", new String[]{KettleVFS.getFriendlyURI(parentfolder)}));
                    }
                } else {
                    if (this.isDetailed()) {
                        this.logDetailed(BaseMessages.getString(PKG, "TextFileOutput.Log.ParentFolderNotExist", new String[]{KettleVFS.getFriendlyURI(parentfolder)}));
                    }

                    if (!this.meta.isCreateParentFolder()) {
                        throw new KettleException(BaseMessages.getString(PKG, "TextFileOutput.Log.ParentFolderNotExistCreateIt", new String[]{KettleVFS.getFriendlyURI(parentfolder), KettleVFS.getFriendlyURI(filename)}));
                    }

                    parentfolder.createFolder();
                    if (this.isDetailed()) {
                        this.logDetailed(BaseMessages.getString(PKG, "TextFileOutput.Log.ParentFolderCreated", new String[]{KettleVFS.getFriendlyURI(parentfolder)}));
                    }
                }
            } finally {
                if (parentfolder != null) {
                    try {
                        parentfolder.close();
                    } catch (Exception var9) {
                    }
                }

            }

        }

        boolean isWriteEnclosureForFieldName(ValueMetaInterface v, String fieldName) {
            return this.isWriteEnclosed(v) || this.isEnclosureFixDisabledAndContainsSeparatorOrEnclosure(fieldName.getBytes());
        }

        boolean isWriteEnclosureForValueMetaInterface(ValueMetaInterface v) {
            return this.isWriteEnclosed(v) || this.isEnclosureFixDisabledAndContainsSeparatorOrEnclosure(v.getName().getBytes());
        }

        boolean isWriteEnclosureForWriteField(byte[] str) {
            return this.meta.isEnclosureForced() && !this.meta.isPadded() || this.isEnclosureFixDisabledAndContainsSeparatorOrEnclosure(str);
        }

        boolean isWriteEnclosed(ValueMetaInterface v) {
            return this.meta.isEnclosureForced() && this.data.binaryEnclosure.length > 0 && v != null && v.isString();
        }

        boolean isEnclosureFixDisabledAndContainsSeparatorOrEnclosure(byte[] source) {
            return !this.meta.isEnclosureFixDisabled() && this.containsSeparatorOrEnclosure(source, this.data.binarySeparator, this.data.binaryEnclosure);
        }

        protected FileObject getFileObject(String vfsFilename) throws KettleFileException {
            return KettleVFS.getFileObject(vfsFilename);
        }

        protected FileObject getFileObject(String vfsFilename, VariableSpace space) throws KettleFileException {
            return KettleVFS.getFileObject(vfsFilename, space);
        }

        protected OutputStream getOutputStream(String vfsFilename, VariableSpace space, boolean append) throws KettleFileException {
            return KettleVFS.getOutputStream(vfsFilename, space, append);
        }

        static {
            //FILE_COMPRESSION_TYPE_NONE = TextFileOutputMeta.fileCompressionTypeCodes[0];
            COMPATIBILITY_APPEND_NO_HEADER = "Y".equals(Const.NVL(System.getProperty("KETTLE_COMPATIBILITY_TEXT_FILE_OUTPUT_APPEND_NO_HEADER"), "N"));
        }
    }

