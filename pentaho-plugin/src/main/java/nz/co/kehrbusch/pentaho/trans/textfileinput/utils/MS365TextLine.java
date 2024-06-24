package nz.co.kehrbusch.pentaho.trans.textfileinput.utils;

import nz.co.kehrbusch.ms365.interfaces.entities.IStreamProvider;
import org.apache.commons.vfs2.FileObject;

public class MS365TextLine {
    String line;
    long lineNumber;
    IStreamProvider file;

    public MS365TextLine(String line, long lineNumber, IStreamProvider file) {
        this.line = line;
        this.lineNumber = lineNumber;
        this.file = file;
    }

    public String getLine() {
        return this.line;
    }

    public void setLine(String line) {
        this.line = line;
    }

    public long getLineNumber() {
        return this.lineNumber;
    }

    public IStreamProvider getFile() {
        return this.file;
    }

    public void setFile(IStreamProvider file) {
        this.file = file;
    }
}
