package nz.co.kehrbusch.pentaho.trans.textfileinput.utils;

import nz.co.kehrbusch.ms365.interfaces.ISharepointConnection;
import nz.co.kehrbusch.ms365.interfaces.entities.IStreamProvider;
import nz.co.kehrbusch.pentaho.trans.textfileinput.MS365TextFileInputData;
import nz.co.kehrbusch.pentaho.trans.textfileinput.MS365TextFileInputMeta;
import nz.co.kehrbusch.pentaho.util.ms365opensavedialog.providers.MS365File;
import org.pentaho.di.core.compress.CompressionInputStream;
import org.pentaho.di.core.compress.CompressionProvider;
import org.pentaho.di.core.compress.CompressionProviderFactory;
import org.pentaho.di.core.exception.KettleException;
import org.pentaho.di.core.exception.KettleFileException;
import org.pentaho.di.core.logging.LogChannelInterface;
import org.pentaho.di.trans.step.BaseStep;
import org.pentaho.di.trans.steps.file.IBaseFileInputReader;
import org.pentaho.di.trans.steps.file.IBaseFileInputStepControl;
import org.pentaho.di.trans.steps.fileinput.text.*;

import java.io.BufferedInputStream;
import java.io.InputStreamReader;

public class MS365TextFileInputReader implements IBaseFileInputReader {
    private static final int BUFFER_SIZE_INPUT_STREAM = 8192;
    private final IBaseFileInputStepControl step;
    private final MS365TextFileInputMeta meta;
    private final MS365TextFileInputData data;
    private final LogChannelInterface log;
    private final CompressionInputStream in;
    private final BufferedInputStreamReader isr;
    protected long lineInFile;
    private boolean first;
    protected long lineNumberInFile;

    public MS365TextFileInputReader(ISharepointConnection iSharepointConnection, IBaseFileInputStepControl step, MS365TextFileInputMeta meta, MS365TextFileInputData data, IStreamProvider file, LogChannelInterface log) throws Exception {
        this.step = step;
        this.meta = meta;
        this.data = data;
        this.log = log;
        CompressionProvider provider = CompressionProviderFactory.getInstance().getCompressionProviderByName(meta.content.fileCompression);
        if (log.isDetailed()) {
            log.logDetailed("This is a compressed file being handled by the " + provider.getName() + " provider");
        }

        this.in = provider.createInputStream(file.getInputStream(iSharepointConnection));
        this.in.nextEntry();
        BufferedInputStream inStream = new BufferedInputStream(this.in, 8192);
        BOMDetector bom = new BOMDetector(inStream);
        if (bom.bomExist()) {
            this.isr = new BufferedInputStreamReader(new InputStreamReader(inStream, bom.getCharset()));
        } else if (meta.getEncoding() != null && meta.getEncoding().length() > 0) {
            this.isr = new BufferedInputStreamReader(new InputStreamReader(inStream, meta.getEncoding()));
        } else {
            this.isr = new BufferedInputStreamReader(new InputStreamReader(inStream));
        }

        String encoding = this.isr.getEncoding();
        data.encodingType = EncodingType.guessEncodingType(encoding);
        this.readInitial();
    }

    protected void readInitial() throws Exception {
        this.data.doneWithHeader = !this.meta.content.header;
        this.data.doneReading = false;
        int bufferSize = 1;
        bufferSize = bufferSize + (this.meta.content.header ? this.meta.content.nrHeaderLines : 0);
        bufferSize += this.meta.content.layoutPaged ? this.meta.content.nrLinesPerPage * (Math.max(0, this.meta.content.nrWraps) + 1) : Math.max(0, this.meta.content.nrWraps);
        bufferSize += this.meta.content.footer ? this.meta.content.nrFooterLines : 0;
        if (this.meta.content.layoutPaged) {
            this.lineNumberInFile = TextFileInputUtils.skipLines(this.log, this.isr, this.data.encodingType, this.data.fileFormatType, this.data.lineStringBuilder, this.meta.content.nrLinesDocHeader, this.meta.getEnclosure(), this.meta.getEscapeCharacter(), this.lineNumberInFile);
        }

        for(int i = 0; i < bufferSize && !this.data.doneReading; ++i) {
            boolean wasNotFiltered = this.tryToReadLine(!this.meta.content.header || i >= this.meta.content.nrHeaderLines);
            if (!wasNotFiltered) {
                ++bufferSize;
            }
        }

        this.data.headerLinesRead = 0;
        this.data.footerLinesRead = 0;
        this.data.pageLinesRead = 0;
    }

    public boolean readRow() throws KettleException {
        Object[] r = null;
        boolean retval = true;
        boolean putrow = false;
        int i;
        if (!this.data.doneReading) {
            int repeats = 1;
            if (this.meta.content.lineWrapped) {
                repeats = this.meta.content.nrWraps > 0 ? this.meta.content.nrWraps : repeats;
            }

            if (!this.data.doneWithHeader && this.data.headerLinesRead == 0) {
                repeats += this.meta.content.nrHeaderLines + 1;
            }

            for(i = 0; i < repeats && !this.data.doneReading; ++i) {
                if (!this.tryToReadLine(true)) {
                    ++repeats;
                }
            }
        }

        if (this.data.lineBuffer.isEmpty()) {
            return false;
        } else {
            MS365TextLine textLine = (MS365TextLine) this.data.lineBuffer.get(0);
            this.step.incrementLinesInput();
            this.data.lineBuffer.remove(0);
            String extra;
            long useNumber;
            if (this.meta.content.layoutPaged) {
                if (!this.data.doneWithHeader && this.data.pageLinesRead == 0) {
                    if (this.log.isRowLevel()) {
                        this.log.logRowlevel("P-HEADER (" + this.data.headerLinesRead + ") : " + textLine.line);
                    }

                    ++this.data.headerLinesRead;
                    if (this.data.headerLinesRead >= this.meta.content.nrHeaderLines) {
                        this.data.doneWithHeader = true;
                    }
                } else if (this.data.pageLinesRead < this.meta.content.nrLinesPerPage) {
                    if (this.meta.content.lineWrapped) {
                        for(i = 0; i < this.meta.content.nrWraps; ++i) {
                            extra = "";
                            if (this.data.lineBuffer.size() > 0) {
                                extra = ((MS365TextLine)this.data.lineBuffer.get(0)).line;
                                this.data.lineBuffer.remove(0);
                            }

                            textLine.line = textLine.line + extra;
                        }
                    }

                    if (this.log.isRowLevel()) {
                        this.log.logRowlevel("P-DATA: " + textLine.line);
                    }

                    ++this.data.pageLinesRead;
                    ++this.lineInFile;
                    useNumber = this.meta.content.rowNumberByFile ? this.lineInFile : this.step.getLinesWritten() + 1L;
                    r = MS365TextFileInputUtils.convertLineToRow(this.log, textLine, this.meta, this.data.currentPassThruFieldsRow, this.data.nrPassThruFields, this.data.outputRowMeta, this.data.convertRowMeta, this.data.filename, useNumber, this.data.separator, this.data.enclosure, this.data.escapeCharacter, this.data.dataErrorLineHandler, this.meta.additionalOutputFields, this.data.shortFilename, this.data.path, this.data.hidden, this.data.lastModificationDateTime, this.data.uriName, this.data.rootUriName, this.data.extension, this.data.size);
                    if (r != null) {
                        putrow = true;
                    }

                    if (!this.meta.content.footer && this.data.pageLinesRead == this.meta.content.nrLinesPerPage) {
                        this.data.doneWithHeader = false;
                        this.data.headerLinesRead = 0;
                        this.data.pageLinesRead = 0;
                        this.data.footerLinesRead = 0;
                        if (this.log.isRowLevel()) {
                            this.log.logRowlevel("RESTART PAGE");
                        }
                    }
                } else {
                    if (this.meta.content.footer && this.data.footerLinesRead < this.meta.content.nrFooterLines) {
                        if (this.log.isRowLevel()) {
                            this.log.logRowlevel("P-FOOTER: " + textLine.line);
                        }

                        ++this.data.footerLinesRead;
                    }

                    if (!this.meta.content.footer || this.data.footerLinesRead >= this.meta.content.nrFooterLines) {
                        this.data.doneWithHeader = false;
                        this.data.headerLinesRead = 0;
                        this.data.pageLinesRead = 0;
                        this.data.footerLinesRead = 0;
                        if (this.log.isRowLevel()) {
                            this.log.logRowlevel("RESTART PAGE");
                        }
                    }
                }
            } else if (!this.data.doneWithHeader) {
                ++this.data.headerLinesRead;
                if (this.data.headerLinesRead >= this.meta.content.nrHeaderLines) {
                    this.data.doneWithHeader = true;
                }
            } else if (this.data.doneReading && this.meta.content.footer && this.data.lineBuffer.size() < this.meta.content.nrFooterLines) {
                this.data.lineBuffer.clear();
            } else {
                if (this.meta.content.lineWrapped) {
                    for(i = 0; i < this.meta.content.nrWraps; ++i) {
                        extra = "";
                        if (this.data.lineBuffer.size() > 0) {
                            extra = ((MS365TextLine)this.data.lineBuffer.get(0)).line;
                            this.data.lineBuffer.remove(0);
                        } else {
                            this.tryToReadLine(true);
                            if (!this.data.lineBuffer.isEmpty()) {
                                extra = ((MS365TextLine)this.data.lineBuffer.remove(0)).line;
                            }
                        }

                        textLine.line = textLine.line + extra;
                    }
                }

                if (this.data.filePlayList.isProcessingNeeded(((MS365File) textLine.file), textLine.lineNumber, "NO_PARTS")) {
                    ++this.lineInFile;
                    useNumber = this.meta.content.rowNumberByFile ? this.lineInFile : this.step.getLinesWritten() + 1L;
                    r = MS365TextFileInputUtils.convertLineToRow(this.log, textLine, this.meta, this.data.currentPassThruFieldsRow, this.data.nrPassThruFields, this.data.outputRowMeta, this.data.convertRowMeta, this.data.filename, useNumber, this.data.separator, this.data.enclosure, this.data.escapeCharacter, this.data.dataErrorLineHandler, this.meta.additionalOutputFields, this.data.shortFilename, this.data.path, this.data.hidden, this.data.lastModificationDateTime, this.data.uriName, this.data.rootUriName, this.data.extension, this.data.size);
                    if (r != null) {
                        if (this.log.isRowLevel()) {
                            this.log.logRowlevel("Found data row: " + this.data.outputRowMeta.getString(r));
                        }

                        putrow = true;
                    }
                } else {
                    putrow = false;
                }
            }

            if (putrow && r != null) {
                if (this.data.nr_repeats > 0) {
                    if (this.data.previous_row == null) {
                        this.data.previous_row = this.data.outputRowMeta.cloneRow(r);
                    } else {
                        for(i = 0; i < this.meta.inputFields.length; ++i) {
                            if (this.meta.inputFields[i].isRepeated()) {
                                if (r[i] == null) {
                                    r[i] = this.data.previous_row[i];
                                } else {
                                    this.data.previous_row[i] = r[i];
                                }
                            }
                        }
                    }
                }

                if (this.log.isRowLevel()) {
                    this.log.logRowlevel("Putting row: " + this.data.outputRowMeta.getString(r));
                }

                this.step.putRow(this.data.outputRowMeta, r);
                if (this.step.getLinesInput() >= this.meta.content.rowLimit && this.meta.content.rowLimit > 0L) {
                    this.close();
                    return false;
                }
            }

            if (this.step.checkFeedback(this.step.getLinesInput()) && this.log.isBasic()) {
                this.log.logBasic("linenr " + this.step.getLinesInput());
            }

            return retval;
        }
    }

    public void close() {
        try {
            if (this.data.filename != null) {
                this.data.lineBuffer.clear();
                this.step.incrementLinesUpdated();
                if (this.in != null) {
                    BaseStep.closeQuietly(this.in);
                }

                this.isr.close();
                this.data.filename = null;
                if (this.data.file != null) {
                    try {
                        this.data.file.disposeInputStream();
                        this.data.file = null;
                    } catch (Exception var3) {
                        this.log.logError("Error closing file", var3);
                    }

                    this.data.file = null;
                }
            }

            this.data.dataErrorLineHandler.close();
        } catch (Exception var4) {
            String errorMsg = "Couldn't close file : " + this.data.file.getName() + " --> " + var4.toString();
            this.log.logError(errorMsg);
            if (this.step.failAfterBadFile(errorMsg)) {
                this.step.stopAll();
            }

            this.step.setErrors(this.step.getErrors() + 1L);
        }

    }

    protected boolean tryToReadLine(boolean applyFilter) throws KettleFileException {
        MS365TextLine textFileLine = MS365TextFileInputUtils.getLine(this.log, this.isr, this.data.encodingType, this.data.fileFormatType, this.data.lineStringBuilder, this.meta.getEnclosure(), this.meta.getEscapeCharacter(), this.lineNumberInFile);
        String line = textFileLine.line;
        this.lineNumberInFile = textFileLine.lineNumber;
        if (line != null) {
            if (applyFilter) {
                boolean isFilterLastLine = false;
                boolean filterOK = this.checkFilterRow(line, isFilterLastLine);
                if (!filterOK) {
                    return false;
                }

                this.data.lineBuffer.add(new MS365TextLine(line, (long)(this.lineNumberInFile++), this.data.file));
            } else if (!this.meta.content.noEmptyLines || line.length() != 0) {
                this.data.lineBuffer.add(new MS365TextLine(line, (long)(this.lineNumberInFile++), this.data.file));
            }
        } else {
            this.data.doneReading = true;
        }

        return true;
    }

    private boolean checkFilterRow(String line, boolean isFilterLastLine) {
        boolean filterOK = true;
        if (this.meta.content.noEmptyLines && line.length() == 0) {
            filterOK = false;
        } else {
            filterOK = this.data.filterProcessor.doFilters(line);
            if (!filterOK && this.data.filterProcessor.isStopProcessing()) {
                this.data.doneReading = true;
            }
        }

        return filterOK;
    }
}
