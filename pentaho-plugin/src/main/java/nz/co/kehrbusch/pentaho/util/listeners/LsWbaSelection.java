package nz.co.kehrbusch.pentaho.util.listeners;

import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.pentaho.di.trans.steps.fileinput.text.TextFileInputMeta;
import org.pentaho.di.ui.core.widget.TableView;
import org.pentaho.di.ui.core.widget.TextVar;

public class LsWbaSelection implements SelectionListener {
    private final WbaSelectionInput wbaSelectionInput;

    public LsWbaSelection(WbaSelectionInput wbaSelectionInput){
        this.wbaSelectionInput = wbaSelectionInput;
    }

    @Override
    public void widgetSelected(SelectionEvent selectionEvent) {
        wbaSelectionInput.getwFilenameList().add(wbaSelectionInput.getFilename().getText(), wbaSelectionInput.getwFilemask().getText(), wbaSelectionInput.getwExcludeFilemask().getText(), TextFileInputMeta.RequiredFilesCode[0]);
        wbaSelectionInput.getFilename().setText("");
        wbaSelectionInput.getwFilemask().setText("");
        wbaSelectionInput.getwExcludeFilemask().setText("");
        wbaSelectionInput.getwFilenameList().removeEmptyRows();
        wbaSelectionInput.getwFilenameList().setRowNums();
        wbaSelectionInput.getwFilenameList().optWidth(true);
    }

    @Override
    public void widgetDefaultSelected(SelectionEvent selectionEvent) {

    }

    public interface WbaSelectionInput {
        TextVar getFilename();
        TableView getwFilenameList();
        TextVar getwFilemask();
        TextVar getwExcludeFilemask();
    }
}
