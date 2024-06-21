package nz.co.kehrbusch.pentaho.trans.textfileoutput;

import org.pentaho.di.core.compress.CompressionOutputStream;
import org.pentaho.di.core.row.RowMetaInterface;
import org.pentaho.di.core.row.ValueMetaInterface;
import org.pentaho.di.trans.step.BaseStepData;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.text.*;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;

public class MS365TextFileOutputData extends BaseStepData {
    public int splitnr;
    public int[] fieldnrs;
    public NumberFormat nf = NumberFormat.getInstance();
    public DecimalFormat df;
    public DecimalFormatSymbols dfs;
    public SimpleDateFormat daf;
    public DateFormatSymbols dafs;
    public CompressionOutputStream out;
    public OutputStream writer;
    public DecimalFormat defaultDecimalFormat;
    public DecimalFormatSymbols defaultDecimalFormatSymbols;
    public SimpleDateFormat defaultDateFormat;
    public DateFormatSymbols defaultDateFormatSymbols;
    public Process cmdProc;
    public RowMetaInterface outputRowMeta;
    public RowMetaInterface inputRowMeta;
    public OutputStream fos;
    public byte[] binarySeparator;
    public byte[] binaryEnclosure;
    public byte[] binaryNewline;
    public boolean hasEncoding;
    public byte[][] binaryNullValue;
    public boolean oneFileOpened;
    public int fileNameFieldIndex;
    public ValueMetaInterface fileNameMeta;
    public IFileStreamsCollection fileStreamsCollection;
    public long lastFileFlushTime = 0L;
    public String fileName;
    public int splitEvery;

    public MS365TextFileOutputData() {
        this.df = (DecimalFormat)this.nf;
        this.dfs = new DecimalFormatSymbols();
        this.daf = new SimpleDateFormat();
        this.dafs = new DateFormatSymbols();
        this.defaultDecimalFormat = (DecimalFormat)NumberFormat.getInstance();
        this.defaultDecimalFormatSymbols = new DecimalFormatSymbols();
        this.defaultDateFormat = new SimpleDateFormat();
        this.defaultDateFormatSymbols = new DateFormatSymbols();
        this.fileNameFieldIndex = -1;
        this.cmdProc = null;
        this.oneFileOpened = false;
    }

    public IFileStreamsCollection getFileStreamsCollection() {
        if (this.fileStreamsCollection == null) {
            if (this.splitnr > 0) {
                this.fileStreamsCollection = new FileStreamsList();
            } else {
                this.fileStreamsCollection = new FileStreamsMap();
            }
        }

        return this.fileStreamsCollection;
    }

    public class FileStreamsMap implements IFileStreamsCollection {
        private int numOpenFiles = 0;
        private TreeMap<String, FileStreamsCollectionEntry> fileNameMap = new TreeMap();
        private TreeMap<Long, FileStreamsCollectionEntry> indexMap = new TreeMap();

        public FileStreamsMap() {
        }

        public int size() {
            return this.fileNameMap.size();
        }

        public void add(String fileName, FileStream fileWriterOutputStream) {
            long index = 0L;
            if (this.size() > 0) {
                index = (Long)this.indexMap.lastKey() + 1L;
            }

            FileStreamsCollectionEntry newEntry = new FileStreamsCollectionEntry(fileName, index, fileWriterOutputStream);
            this.fileNameMap.put(fileName, newEntry);
            this.indexMap.put(index, newEntry);
            if (fileWriterOutputStream.isOpen()) {
                ++this.numOpenFiles;
            }

        }

        public FileStream getStream(String filename) {
            return this.fileNameMap.containsKey(filename) ? ((FileStreamsCollectionEntry)this.fileNameMap.get(filename)).getFileStream() : null;
        }

        public String getLastFileName() {
            String filename = null;
            if (this.indexMap.size() > 0) {
                filename = ((FileStreamsCollectionEntry)this.indexMap.lastEntry().getValue()).getFileName();
            }

            return filename;
        }

        public FileStream getLastStream() {
            FileStream lastStream = null;
            if (this.indexMap.size() > 0) {
                lastStream = ((FileStreamsCollectionEntry)this.indexMap.lastEntry().getValue()).getFileStream();
            }

            return lastStream;
        }

        public int getNumOpenFiles() {
            return this.numOpenFiles;
        }

        public void closeOldestOpenFile(boolean removeFileFromCollection) throws IOException {
            FileStream oldestOpenStream = null;
            String oldestOpenFileName = null;
            Long oldestOpenFileIndex = null;
            Iterator var5 = this.indexMap.entrySet().iterator();

            while(var5.hasNext()) {
                Map.Entry<Long, FileStreamsCollectionEntry> mapEntry = (Map.Entry)var5.next();
                FileStreamsCollectionEntry existingStream = (FileStreamsCollectionEntry)mapEntry.getValue();
                if (existingStream.getFileStream().isOpen()) {
                    oldestOpenStream = existingStream.getFileStream();
                    oldestOpenFileName = existingStream.getFileName();
                    oldestOpenFileIndex = existingStream.getIndex();
                    break;
                }
            }

            if (oldestOpenStream != null) {
                oldestOpenStream.flush();
                oldestOpenStream.close();
                --this.numOpenFiles;
                if (removeFileFromCollection) {
                    this.fileNameMap.remove(oldestOpenFileName);
                    this.indexMap.remove(oldestOpenFileIndex);
                }
            }

        }

        public void flushOpenFiles(boolean closeAfterFlush) {
            Iterator var2 = this.indexMap.values().iterator();

            while(var2.hasNext()) {
                FileStreamsCollectionEntry collectionEntry = (FileStreamsCollectionEntry)var2.next();
                if (collectionEntry.getFileStream().isDirty()) {
                    try {
                        collectionEntry.getFileStream().flush();
                        if (closeAfterFlush) {
                            collectionEntry.getFileStream().close();
                        }
                    } catch (IOException var5) {
                        var5.printStackTrace();
                    }
                }
            }

        }

        public void closeFile(String filename) throws IOException {
            FileStream outputStreams = this.getStream(filename);
            if (outputStreams != null && outputStreams.isOpen()) {
                outputStreams.flush();
                outputStreams.close();
                --this.numOpenFiles;
            }

        }

        public void closeStream(OutputStream outputStream) throws IOException {
            Iterator var2 = this.indexMap.entrySet().iterator();

            while(true) {
                Map.Entry mapEntry;
                FileStream fileStream;
                do {
                    if (!var2.hasNext()) {
                        return;
                    }

                    mapEntry = (Map.Entry)var2.next();
                    fileStream = ((FileStreamsCollectionEntry)mapEntry.getValue()).getFileStream();
                } while(fileStream.getBufferedOutputStream() != outputStream);

                this.closeFile(((FileStreamsCollectionEntry)mapEntry.getValue()).getFileName());
            }
        }
    }

    public class FileStreamsList implements IFileStreamsCollection {
        ArrayList<FileStream> streamsList = new ArrayList();
        ArrayList<String> namesList = new ArrayList();
        int numOpenFiles = 0;

        public FileStreamsList() {
        }

        public FileStream getStream(String filename) {
            int index = this.namesList.indexOf(filename);
            return index == -1 ? null : this.streamsList.get(index);
        }

        public void closeOldestOpenFile(boolean removeFileFromCollection) throws IOException {
            FileStream oldestOpenStream = null;

            int i;
            for(i = 0; i < this.streamsList.size(); ++i) {
                FileStream existingStream = this.streamsList.get(i);
                if (existingStream.isOpen()) {
                    oldestOpenStream = existingStream;
                    break;
                }
            }

            if (oldestOpenStream != null) {
                oldestOpenStream.flush();
                oldestOpenStream.close();
                --this.numOpenFiles;
                if (removeFileFromCollection) {
                    this.streamsList.remove(i);
                    this.namesList.remove(i);
                }
            }

        }

        public void flushOpenFiles(boolean closeAfterFlush) throws IOException {
            Iterator var2 = this.streamsList.iterator();

            while(var2.hasNext()) {
                FileStream outputStream = (FileStream)var2.next();
                if (outputStream.isDirty()) {
                    try {
                        outputStream.flush();
                        if (closeAfterFlush && outputStream.isOpen()) {
                            outputStream.close();
                            --this.numOpenFiles;
                        }
                    } catch (IOException var5) {
                        var5.printStackTrace();
                    }
                }
            }

        }

        public String getLastFileName() {
            return this.namesList.isEmpty() ? null : (String)this.namesList.get(this.namesList.size() - 1);
        }

        public FileStream getLastStream() {
            return this.streamsList.isEmpty() ? null : this.streamsList.get(this.streamsList.size() - 1);
        }

        public int getNumOpenFiles() {
            return this.numOpenFiles;
        }

        public void closeFile(String filename) throws IOException {
            int index = this.namesList.indexOf(filename);
            if (index >= 0) {
                FileStream existingStream = this.streamsList.get(index);
                if (existingStream.isOpen()) {
                    existingStream.flush();
                    existingStream.close();
                    --this.numOpenFiles;
                }
            }

        }

        public void closeStream(OutputStream outputStream) throws IOException {
            for(int i = 0; i < this.streamsList.size(); ++i) {
                FileStream fileStream = (FileStream)this.streamsList.get(i);
                if (fileStream.getBufferedOutputStream() == outputStream) {
                    this.closeFile((String)this.namesList.get(i));
                }
            }

        }

        public int size() {
            return this.streamsList.size();
        }

        public void add(String filename, FileStream fileStreams) {
            this.namesList.add(filename);
            this.streamsList.add(fileStreams);
            if (fileStreams.isOpen()) {
                ++this.numOpenFiles;
            }

        }
    }

    public static class FileStream {
        BufferedOutputStream bufferedOutputStream;
        boolean isDirty;

        public FileStream(BufferedOutputStream bufferedOutputStream) {
            this.bufferedOutputStream = bufferedOutputStream;
            this.isDirty = false;
        }

        public boolean isDirty() {
            return this.isDirty;
        }

        public void setDirty(boolean dirty) {
            this.isDirty = dirty;
        }

        public void flush() throws IOException {
            if (this.isDirty) {
                this.getBufferedOutputStream().flush();
                this.isDirty = false;
            }

        }

        public void close() throws IOException {
            this.bufferedOutputStream.close();
            this.setBufferedOutputStream((BufferedOutputStream)null);
            this.isDirty = false;
        }

        public boolean isOpen() {
            return this.bufferedOutputStream != null;
        }

        public BufferedOutputStream getBufferedOutputStream() {
            return this.bufferedOutputStream;
        }

        public void setBufferedOutputStream(BufferedOutputStream outputStream) {
            this.bufferedOutputStream = outputStream;
        }
    }

    public class FileStreamsCollectionEntry {
        private String fileName;
        private long index = 0L;
        private FileStream fileStream;

        public FileStreamsCollectionEntry(String fileName, long index, FileStream fileStream) {
            this.fileName = fileName;
            this.index = index;
            this.fileStream = fileStream;
        }

        public String getFileName() {
            return this.fileName;
        }

        public void setFileName(String fileName) {
            this.fileName = fileName;
        }

        public long getIndex() {
            return this.index;
        }

        public void setIndex(int index) {
            this.index = (long)index;
        }

        public FileStream getFileStream() {
            return this.fileStream;
        }

        public void setFileStream(FileStream fileStream) {
            this.fileStream = fileStream;
        }
    }

    public interface IFileStreamsCollection {
        FileStream getStream(String var1);

        void closeOldestOpenFile(boolean var1) throws IOException;

        void flushOpenFiles(boolean var1) throws IOException;

        String getLastFileName();

        FileStream getLastStream();

        int getNumOpenFiles();

        void closeFile(String var1) throws IOException;

        void closeStream(OutputStream var1) throws IOException;

        int size();

        void add(String var1, FileStream var2);
    }
}
