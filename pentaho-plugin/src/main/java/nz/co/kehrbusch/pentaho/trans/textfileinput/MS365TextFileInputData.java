package nz.co.kehrbusch.pentaho.trans.textfileinput;

import nz.co.kehrbusch.pentaho.trans.textfileinput.replay.MS365FilePlayListReplay;
import nz.co.kehrbusch.pentaho.trans.textfileinput.utils.MS365TextLine;
import nz.co.kehrbusch.pentaho.util.file.SharepointFileWrapper;
import nz.co.kehrbusch.pentaho.util.ms365opensavedialog.providers.MS365File;
import org.pentaho.di.core.row.RowMetaInterface;
import org.pentaho.di.trans.step.BaseStepData;
import org.pentaho.di.trans.step.errorhandling.FileErrorHandler;
import org.pentaho.di.trans.steps.file.IBaseFileInputReader;
import org.pentaho.di.trans.steps.fileinput.text.EncodingType;
import org.pentaho.di.trans.steps.fileinput.text.TextFileFilterProcessor;

import java.util.*;

public class MS365TextFileInputData extends BaseStepData {
    public FileErrorHandler dataErrorLineHandler;
    public SharepointFileWrapper<MS365TextFileInputMeta> files;
    public String filename;
    public int currentFileIndex;
    public MS365File file;
    public IBaseFileInputReader reader;
    public RowMetaInterface outputRowMeta;
    public HashMap<String, Object[]> passThruFields;
    public Object[] currentPassThruFieldsRow;
    public int nrPassThruFields;
    public RowMetaInterface convertRowMeta;
    public int nr_repeats;
    public Map<String, Boolean> rejectedFiles = new HashMap();
    public String shortFilename;
    public String path;
    public String extension;
    public boolean hidden;
    public Date lastModificationDateTime;
    public String uriName;
    public String rootUriName;
    public Long size;
    public List<MS365TextLine> lineBuffer = new LinkedList();
    public Object[] previous_row;
    public int nrLinesOnPage;
    public boolean doneReading;
    public int headerLinesRead;
    public int footerLinesRead;
    public int pageLinesRead;
    public boolean doneWithHeader;
    public MS365FilePlayListReplay filePlayList;
    public TextFileFilterProcessor filterProcessor;
    public StringBuilder lineStringBuilder;
    public int fileFormatType;
    public int fileType;
    public String separator;
    public String enclosure;
    public String escapeCharacter;
    public EncodingType encodingType;

    public MS365TextFileInputData() {
        this.nr_repeats = 0;
        this.previous_row = null;
        this.nrLinesOnPage = 0;
        this.filterProcessor = null;
        this.lineStringBuilder = new StringBuilder(256);
    }
}
