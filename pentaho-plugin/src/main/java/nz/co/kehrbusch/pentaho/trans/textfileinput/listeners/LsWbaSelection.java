package nz.co.kehrbusch.pentaho.trans.textfileinput.listeners;

import nz.co.kehrbusch.pentaho.trans.textfileinput.MS365TextFileInputDialog;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.pentaho.di.trans.steps.fileinput.text.TextFileInputMeta;

public class LsWbaSelection implements SelectionListener {
    private final MS365TextFileInputDialog ms365TextFileInputDialog;

    public LsWbaSelection(MS365TextFileInputDialog ms365TextFileInputDialog){
        this.ms365TextFileInputDialog = ms365TextFileInputDialog;
    }

    @Override
    public void widgetSelected(SelectionEvent selectionEvent) {
        ms365TextFileInputDialog.getwFilenameList().add(ms365TextFileInputDialog.getFilename().getText(), ms365TextFileInputDialog.getwFilemask().getText(), ms365TextFileInputDialog.getwExcludeFilemask().getText(), TextFileInputMeta.RequiredFilesCode[0]);
        ms365TextFileInputDialog.getFilename().setText("");
        ms365TextFileInputDialog.getwFilemask().setText("");
        ms365TextFileInputDialog.getwExcludeFilemask().setText("");
        ms365TextFileInputDialog.getwFilenameList().removeEmptyRows();
        ms365TextFileInputDialog.getwFilenameList().setRowNums();
        ms365TextFileInputDialog.getwFilenameList().optWidth(true);
    }

    @Override
    public void widgetDefaultSelected(SelectionEvent selectionEvent) {

    }
}
