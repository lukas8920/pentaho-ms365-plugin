package nz.co.kehrbusch.pentaho.trans.getfilenames;

import nz.co.kehrbusch.ms365.interfaces.entities.IStreamProvider;
import nz.co.kehrbusch.pentaho.trans.textfileinput.replay.MS365FilePlayListReplay;
import nz.co.kehrbusch.pentaho.util.file.SharepointFileWrapper;
import nz.co.kehrbusch.pentaho.util.ms365opensavedialog.providers.MS365File;
import org.apache.commons.vfs2.FileObject;
import org.pentaho.di.core.fileinput.FileInputList;
import org.pentaho.di.core.playlist.FilePlayList;
import org.pentaho.di.core.row.RowMetaInterface;
import org.pentaho.di.trans.step.errorhandling.FileErrorHandler;
import org.pentaho.di.trans.steps.getfilenames.GetFileNamesData;

import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.text.*;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipInputStream;

public class MS365GetFileNamesData extends GetFileNamesData {
    public List<String> lineBuffer = new ArrayList();
    public Object[] previous_row;
    public int nr_repeats;
    public int nrLinesOnPage;
    public NumberFormat nf = NumberFormat.getInstance();
    public DecimalFormat df;
    public DecimalFormatSymbols dfs;
    public SimpleDateFormat daf;
    public RowMetaInterface outputRowMeta;
    public DateFormatSymbols dafs;
    public SharepointFileWrapper<MS365GetFileNamesMeta> files;
    public boolean isLastFile;
    public String filename;
    public int filenr;
    public int filessize;
    public ByteArrayInputStream fr;
    public ZipInputStream zi;
    public InputStreamReader isr;
    public boolean doneReading;
    public int headerLinesRead;
    public int footerLinesRead;
    public int pageLinesRead;
    public boolean doneWithHeader;
    public FileErrorHandler dataErrorLineHandler;
    public MS365FilePlayListReplay filePlayList;
    public MS365File file;
    public long rownr;
    public int totalpreviousfields;
    public int indexOfFilenameField;
    public int indexOfWildcardField;
    public int indexOfExcludeWildcardField;
    public RowMetaInterface inputRowMeta;
    public Object[] readrow;
    public int nrStepFields;

    public MS365GetFileNamesData() {
        this.df = (DecimalFormat)this.nf;
        this.dfs = new DecimalFormatSymbols();
        this.daf = new SimpleDateFormat();
        this.dafs = new DateFormatSymbols();
        this.nr_repeats = 0;
        this.previous_row = null;
        this.filenr = 0;
        this.filessize = 0;
        this.nrLinesOnPage = 0;
        this.fr = null;
        this.zi = null;
        this.file = null;
        this.totalpreviousfields = 0;
        this.indexOfFilenameField = -1;
        this.indexOfWildcardField = -1;
        this.readrow = null;
        this.nrStepFields = 0;
        this.indexOfExcludeWildcardField = -1;
    }

    public void setDateFormatLenient(boolean lenient) {
        this.daf.setLenient(lenient);
    }
}
