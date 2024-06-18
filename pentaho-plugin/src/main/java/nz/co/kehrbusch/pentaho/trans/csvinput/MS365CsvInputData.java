package nz.co.kehrbusch.pentaho.trans.csvinput;

import nz.co.kehrbusch.pentaho.util.file.PositionableByteInputStream;
import org.pentaho.di.core.exception.KettleException;
import org.pentaho.di.core.row.RowMetaInterface;
import org.pentaho.di.trans.steps.csvinput.CrLfMatcherInterface;
import org.pentaho.di.trans.steps.csvinput.CsvInputData;
import org.pentaho.di.trans.steps.csvinput.FieldsMapping;
import org.pentaho.di.trans.steps.csvinput.PatternMatcherInterface;
import org.pentaho.di.trans.steps.textfileinput.EncodingType;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.List;

public class MS365CsvInputData extends CsvInputData {
    public PositionableByteInputStream fc;
    public ByteBuffer bb;
    public RowMetaInterface convertRowMeta;
    public RowMetaInterface outputRowMeta;
    private byte[] byteBuffer = new byte[0];
    private int startBuffer = 0;
    private int endBuffer = 0;
    private int bufferSize;
    public byte[] delimiter;
    public byte[] enclosure;
    public int preferredBufferSize;
    public String[] filenames;
    public int filenr;
    public int startFilenr;
    public byte[] binaryFilename;
    public InputStream fis;
    public boolean isAddingRowNumber;
    public long rowNumber;
    public boolean stopReading;
    public int stepNumber;
    public int totalNumberOfSteps;
    public List<Long> fileSizes;
    public long totalFileSize;
    public long blockToRead;
    public long startPosition;
    public long endPosition;
    public long bytesToSkipInFirstFile;
    public long totalBytesRead = 0L;
    public boolean parallel;
    public int filenameFieldIndex;
    public int rownumFieldIndex;
    public EncodingType encodingType;
    public PatternMatcherInterface delimiterMatcher;
    public PatternMatcherInterface enclosureMatcher;
    public CrLfMatcherInterface crLfMatcher;
    public FieldsMapping fieldsMapping;

    public MS365CsvInputData() {
    }

    private void resizeByteBufferArray() {
        this.bufferSize = this.endBuffer - this.startBuffer;
        int newSize = this.bufferSize + this.preferredBufferSize;
        byte[] newByteBuffer = new byte[newSize + 100];
        System.arraycopy(this.byteBuffer, this.startBuffer, newByteBuffer, 0, this.bufferSize);
        this.byteBuffer = newByteBuffer;
        this.startBuffer = 0;
        this.endBuffer = this.bufferSize;
    }

    private int readBufferFromFile() throws IOException {
        if (this.endBuffer >= this.bb.capacity()) {
            this.resizeByteBuffer((int)((double)this.bb.capacity() * 1.5D));
        }

        this.bb.position(this.endBuffer);
        int n = this.fc.read(this.bb);
        if (n >= 0) {
            this.bufferSize = this.endBuffer + n;
            if (this.byteBuffer.length < this.bufferSize) {
                byte[] newByteBuffer = new byte[this.bufferSize];
                System.arraycopy(this.byteBuffer, 0, newByteBuffer, 0, this.byteBuffer.length);
                this.byteBuffer = newByteBuffer;
            }

            this.bb.position(this.endBuffer);
            this.bb.get(this.byteBuffer, this.endBuffer, n);
        }

        return n;
    }

    private void resizeByteBuffer(int newSize) {
        ByteBuffer newBuffer = ByteBuffer.allocateDirect(newSize);
        newBuffer.position(0);
        newBuffer.put(this.bb);
        this.bb = newBuffer;
    }

    boolean resizeBufferIfNeeded() throws IOException {
        if (this.endOfBuffer()) {
            this.resizeByteBufferArray();
            int n = this.readBufferFromFile();
            return n < 0;
        } else {
            return false;
        }
    }

    boolean moveEndBufferPointer() throws IOException {
        return this.moveEndBufferPointer(true);
    }

    void moveEndBufferPointerXTimes(int xTimes) throws IOException {
        for(int i = 0; i < xTimes; ++i) {
            this.moveEndBufferPointer(true);
        }

    }

    boolean moveEndBufferPointer(boolean increaseTotalBytes) throws IOException {
        ++this.endBuffer;
        if (increaseTotalBytes) {
            ++this.totalBytesRead;
        }

        return this.resizeBufferIfNeeded();
    }

    byte[] removeEscapedEnclosures(byte[] field, int nrEnclosuresFound) {
        byte[] result = new byte[field.length - nrEnclosuresFound];
        int resultIndex = 0;

        for(int i = 0; i < field.length; ++i) {
            result[resultIndex++] = field[i];
            if (field[i] == this.enclosure[0] && i + 1 < field.length && field[i + 1] == this.enclosure[0]) {
                ++i;
            }
        }

        return result;
    }

    byte[] getField(boolean delimiterFound, boolean enclosureFound, boolean newLineFound, boolean endOfBuffer) {
        int fieldStart = this.startBuffer;
        int fieldEnd = this.endBuffer;
        if (newLineFound && !endOfBuffer) {
            fieldEnd -= this.encodingType.getLength();
        }

        if (enclosureFound) {
            fieldStart += this.enclosure.length;
            fieldEnd -= this.enclosure.length;
        }

        int length = fieldEnd - fieldStart;
        if (length <= 0) {
            length = 0;
        }

        byte[] field = new byte[length];
        System.arraycopy(this.byteBuffer, fieldStart, field, 0, length);
        return field;
    }

    void closeFile() throws KettleException {
        try {
            if (this.fc != null) {
                this.fc.close();
            }

            if (this.fis != null) {
                this.fis.close();
            }

        } catch (IOException var2) {
            throw new KettleException("Unable to close file channel for file '" + this.filenames[this.filenr - 1], var2);
        }
    }

    int getStartBuffer() {
        return this.startBuffer;
    }

    void setStartBuffer(int startBuffer) {
        this.startBuffer = startBuffer;
    }

    int getEndBuffer() {
        return this.endBuffer;
    }

    boolean isCarriageReturn() {
        return this.encodingType.isReturn(this.byteBuffer[this.endBuffer]);
    }

    boolean newLineFound() {
        return this.crLfMatcher.isReturn(this.byteBuffer, this.endBuffer) || this.crLfMatcher.isLineFeed(this.byteBuffer, this.endBuffer);
    }

    boolean delimiterFound() throws IOException {
        this.checkMinimumBytesAvailable(this.delimiter.length);
        return this.delimiterMatcher.matchesPattern(this.byteBuffer, this.endBuffer, this.delimiter);
    }

    boolean enclosureFound() {
        return this.enclosureMatcher.matchesPattern(this.byteBuffer, this.endBuffer, this.enclosure);
    }

    boolean endOfBuffer() {
        return this.endBuffer >= this.bufferSize;
    }

    private void checkMinimumBytesAvailable(int bytesNeeded) throws IOException {
        if (this.bufferSize - this.endBuffer < bytesNeeded) {
            int newSize = this.bufferSize - this.startBuffer + Math.max(this.preferredBufferSize, bytesNeeded);
            byte[] newByteBuffer = new byte[newSize + 100];
            System.arraycopy(this.byteBuffer, this.startBuffer, newByteBuffer, 0, this.bufferSize - this.startBuffer);
            this.byteBuffer = newByteBuffer;
            int newEndBuffer = this.endBuffer - this.startBuffer;
            this.bufferSize = this.endBuffer = this.bufferSize - this.startBuffer;
            this.startBuffer = 0;
            this.readBufferFromFile();
            this.endBuffer = newEndBuffer;
        }

    }
}
