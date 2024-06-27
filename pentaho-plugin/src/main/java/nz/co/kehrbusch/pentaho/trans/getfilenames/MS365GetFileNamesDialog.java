package nz.co.kehrbusch.pentaho.trans.getfilenames;

import nz.co.kehrbusch.pentaho.connections.manage.ConnectionDetailsInterface;
import nz.co.kehrbusch.pentaho.connections.manage.MS365ConnectionManager;
import nz.co.kehrbusch.pentaho.util.listeners.LsSelectFileFolder;
import nz.co.kehrbusch.pentaho.util.listeners.LsShowFiles;
import nz.co.kehrbusch.pentaho.util.listeners.LsWbaSelection;
import nz.co.kehrbusch.pentaho.util.ms365opensavedialog.providers.BaseEntity;
import org.eclipse.swt.custom.CCombo;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.widgets.*;
import org.pentaho.di.core.Const;
import org.pentaho.di.core.logging.LogChannel;
import org.pentaho.di.core.util.Utils;
import org.pentaho.di.i18n.BaseMessages;
import org.pentaho.di.trans.Trans;
import org.pentaho.di.trans.TransMeta;
import org.pentaho.di.trans.TransPreviewFactory;
import org.pentaho.di.ui.core.dialog.EnterNumberDialog;
import org.pentaho.di.ui.core.dialog.EnterTextDialog;
import org.pentaho.di.ui.core.dialog.PreviewRowsDialog;
import org.pentaho.di.ui.core.widget.ColumnInfo;
import org.pentaho.di.ui.core.widget.TableView;
import org.pentaho.di.ui.core.widget.TextVar;
import org.pentaho.di.ui.trans.dialog.TransPreviewProgressDialog;
import org.pentaho.di.ui.trans.steps.getfilenames.GetFileNamesDialog;

import java.util.Arrays;
import java.util.stream.Collectors;

public class MS365GetFileNamesDialog extends GetFileNamesDialog implements LsSelectFileFolder.FileFolderInput, LsWbaSelection.WbaSelectionInput, LsShowFiles.ShowFilesInput<MS365GetFileNamesMeta> {
    private static final Class<?> PKG = MS365GetFileNamesDialog.class;

    private MS365ConnectionManager connectionManager;
    private MS365GetFileNamesMeta ms365GetFileNamesMeta;

    private boolean isInitialized = false;
    private String[] connections;

    private CCombo wConnectionField;
    private TextVar wFilename;
    private TableView wFilenameList;
    private TextVar wExcludeFilemask;
    private TextVar wFilemask;
    private Button wbShowFiles;
    private Label lblLoadingInfo;
    private Button wbInclRownum;
    private TextVar wInclRownumField;
    private CCombo wFilenameField;
    private CCombo wWildcardField;
    private CCombo wExcludeWildcardField;
    private Button wFileField;
    private Text wLimit;

    public MS365GetFileNamesDialog(Shell parent, Object in, TransMeta transMeta, String sname) {
        super(parent, in, transMeta, sname);
        this.connectionManager = MS365ConnectionManager.getInstance(new LogChannel());
        this.ms365GetFileNamesMeta = (MS365GetFileNamesMeta) in;
    }

    @Override
    public String open(){
        this.ms365GetFileNamesMeta.setMs365GetFileNamesDialog(this);
        return super.open();
    }

    public void addControls(){
        Control[] controls = this.shell.getChildren();
        int margin = 4;
        int middle = this.props.getMiddlePct();

        CTabFolder wTabFolder = (CTabFolder) Arrays.stream(controls).filter(control -> control instanceof CTabFolder).collect(Collectors.toList()).get(0);
        Control[] tabChildren = wTabFolder.getChildren();
        Composite wFileComp = (Composite) Arrays.stream(tabChildren).filter(control -> control instanceof Composite).collect(Collectors.toList()).get(1);
        Composite wFilterComp = (Composite) Arrays.stream(tabChildren).filter(control -> control instanceof Composite).collect(Collectors.toList()).get(2);
        Control[] filterChildren = wFilterComp.getChildren();
        Group group = (Group) Arrays.stream(filterChildren).filter(control ->  control instanceof Group).collect(Collectors.toList()).get(0);
        Control[] firstGroupChildren = group.getChildren();
        Group group1 = (Group) Arrays.stream(filterChildren).filter(control -> control instanceof Group).collect(Collectors.toList()).get(1);
        Control[] secondGroupChildren = group1.getChildren();
        Control[] fileChildren = wFileComp.getChildren();
        Group originFiles = (Group) Arrays.stream(fileChildren).filter(control -> control instanceof Group).collect(Collectors.toList()).get(0);
        Control[] originChildren = originFiles.getChildren();

        wFilename = (TextVar) Arrays.stream(fileChildren).filter(control -> control instanceof TextVar).collect(Collectors.toList()).get(0);
        Button wbbFilename = (Button) Arrays.stream(fileChildren).filter(control -> control instanceof Button).collect(Collectors.toList()).get(0);
        Button wbaFilename = (Button) Arrays.stream(fileChildren).filter(control -> control instanceof Button).collect(Collectors.toList()).get(1);
        Label wlFilterFileType = (Label) Arrays.stream(filterChildren).filter(control -> control instanceof Label).collect(Collectors.toList()).get(0);
        CCombo wFilterFileType = (CCombo) Arrays.stream(filterChildren).filter(control -> control instanceof CCombo).collect(Collectors.toList()).get(0);
        Label wlInclRownum = (Label) Arrays.stream(firstGroupChildren).filter(control -> control instanceof Label).collect(Collectors.toList()).get(0);
        wbInclRownum = (Button) Arrays.stream(firstGroupChildren).filter(control -> control instanceof Button).collect(Collectors.toList()).get(0);
        Label wlInclRownumField = (Label) Arrays.stream(firstGroupChildren).filter(control -> control instanceof Label).collect(Collectors.toList()).get(1);
        wInclRownumField = (TextVar) Arrays.stream(firstGroupChildren).filter(control -> control instanceof TextVar).collect(Collectors.toList()).get(0);
        Label wlAddResult = (Label) Arrays.stream(secondGroupChildren).filter(control -> control instanceof Label).collect(Collectors.toList()).get(0);
        Button wAddFileResult = (Button) Arrays.stream(secondGroupChildren).filter(control -> control instanceof Button).collect(Collectors.toList()).get(0);
        wFilenameList = (TableView) Arrays.stream(fileChildren).filter(control -> control instanceof TableView).collect(Collectors.toList()).get(0);
        wExcludeFilemask = (TextVar) Arrays.stream(fileChildren).filter(control -> control instanceof TextVar).collect(Collectors.toList()).get(2);
        wFilemask = (TextVar) Arrays.stream(fileChildren).filter(control -> control instanceof TextVar).collect(Collectors.toList()).get(1);
        wbShowFiles = (Button) Arrays.stream(fileChildren).filter(control -> control instanceof Button).collect(Collectors.toList()).get(4);
        wFilenameField = (CCombo) Arrays.stream(originChildren).filter(control -> control instanceof CCombo).collect(Collectors.toList()).get(0);
        wWildcardField = (CCombo) Arrays.stream(originChildren).filter(control -> control instanceof CCombo).collect(Collectors.toList()).get(1);
        wExcludeWildcardField = (CCombo) Arrays.stream(originChildren).filter(control -> control instanceof CCombo).collect(Collectors.toList()).get(2);
        wFileField = (Button) Arrays.stream(originChildren).filter(control -> control instanceof Button).collect(Collectors.toList()).get(0);
        wLimit = (Text) Arrays.stream(filterChildren).filter(control -> control instanceof Text).collect(Collectors.toList()).get(0);

        this.wPreview.removeListener(13, this.lsPreview);
        this.lsPreview = event -> MS365GetFileNamesDialog.this.preview();
        this.wPreview.addListener(13, this.lsPreview);

        Label wlConnection = new Label(this.shell, 131072);
        wlConnection.setText(BaseMessages.getString(PKG, "MS365GetFileNamesDialog.connection.Label"));
        this.props.setLook(wlConnection);
        FormData fdlConnection = new FormData();
        fdlConnection.top = new FormAttachment(wStepname, margin);
        fdlConnection.left = new FormAttachment(0, 0);
        fdlConnection.right = new FormAttachment(middle, -margin);
        wlConnection.setLayoutData(fdlConnection);
        wConnectionField = new CCombo(this.shell, 18436);
        connections = connectionManager.getConnections().stream().map(ConnectionDetailsInterface::getConnectionName).toArray(String[]::new);
        wConnectionField.setItems(connections);
        this.props.setLook(wConnectionField);
        wConnectionField.addModifyListener(modifyEvent -> {
            this.ms365GetFileNamesMeta.setChanged();
        });
        fdlConnection = new FormData();
        fdlConnection.top = new FormAttachment(wStepname, margin);
        fdlConnection.left = new FormAttachment(middle, 0);
        fdlConnection.right = new FormAttachment(100, 0);
        wConnectionField.setLayoutData(fdlConnection);

        FormData fdTabFolder = (FormData) wTabFolder.getLayoutData();
        fdTabFolder.top = new FormAttachment(wConnectionField, margin);
        wTabFolder.setLayoutData(fdTabFolder);

        FormData fdFilename = (FormData) wbbFilename.getLayoutData();
        wbbFilename.setVisible(false);
        wbbFilename = new Button(wFileComp, 16777224);
        this.props.setLook(wbbFilename);
        wbbFilename.setLayoutData(fdFilename);
        wbbFilename.setText(BaseMessages.getString(PKG, "System.Button.Browse", new String[0]));
        wbbFilename.addSelectionListener(new LsSelectFileFolder(this));

        TableColumn[] columns = wFilenameList.getTable().getColumns();
        columns[5].dispose();
        columns[5] = new TableColumn(wFilenameList.getTable(), 0);
        ColumnInfo[] colInfos = wFilenameList.getColumns();
        colInfos[4].setFieldTypeColumn(0);
        colInfos[4].setReadOnly(true);
        colInfos[4].setDisabledListener(i -> true);
        wFilenameList.getTable().layout();
        wFilenameList.getTable().pack();
        wFilenameList.optWidth(true);
        wFilenameList.layout();
        wFilenameList.pack();

        Listener[] listeners = this.wOK.getListeners(13);
        Listener overwriteListener = event -> {
            MS365GetFileNamesDialog.this.ok();
            listeners[0].handleEvent(event);
        };
        this.wOK.removeListener(13, listeners[0]);
        this.wOK.addListener(13, overwriteListener);

        wlFilterFileType.setVisible(false);
        wFilterFileType.setVisible(false);

        FormData fdRownum = (FormData) wlInclRownum.getLayoutData();
        fdRownum.top = new FormAttachment(0, 3 * margin);
        wlInclRownum.setLayoutData(fdRownum);
        fdRownum = (FormData) wbInclRownum.getLayoutData();
        fdRownum.top = new FormAttachment(0, 3 * margin);
        wbInclRownum.setLayoutData(fdRownum);
        fdRownum = (FormData) wlInclRownumField.getLayoutData();
        fdRownum.top = new FormAttachment(0, 3 * margin);
        wlInclRownumField.setLayoutData(fdRownum);
        fdRownum = (FormData) wInclRownumField.getLayoutData();
        fdRownum.top = new FormAttachment(0, 3 * margin);
        wInclRownumField.setLayoutData(fdRownum);
        fdRownum = (FormData) group.getLayoutData();
        fdRownum.top = new FormAttachment(0, margin);
        group.setLayoutData(fdRownum);

        Listener[] listeners1 = wbaFilename.getListeners(13);
        wbaFilename.removeListener(13, listeners1[0]);
        wbaFilename.addSelectionListener(new LsWbaSelection(this));

        Listener[] listeners2 = wbShowFiles.getListeners(13);
        wbShowFiles.removeListener(13, listeners2[0]);
        wbShowFiles.addListener(13, new LsShowFiles<>(this));

        lblLoadingInfo = new Label(this.shell, 131072);
        lblLoadingInfo.setText(BaseMessages.getString(PKG, "MS365GetFileNamesDialog.Loading.Label"));
        props.setLook(lblLoadingInfo);
        FormData fbdLoading = new FormData();
        fbdLoading.bottom = new FormAttachment(100, -11);
        fbdLoading.left = new FormAttachment(wCancel, 80);
        lblLoadingInfo.setLayoutData(fbdLoading);
        lblLoadingInfo.setVisible(false);

        wlAddResult.setVisible(false);
        wAddFileResult.setVisible(false);
        group1.setVisible(false);

        this.populateDialog();
    }

    public void attachAdditionalFields() {
        if (!this.isInitialized){
            addControls();
            this.isInitialized = true;
        }
    }

    private void ok(){
        if (!Utils.isEmpty(this.wStepname.getText())) {
            this.ms365GetFileNamesMeta.setConnectionName(this.wConnectionField.getText());
        }
    }

    private void populateDialog(){
        if (ms365GetFileNamesMeta.getConnectionName() != null){
            wConnectionField.setText(ms365GetFileNamesMeta.getConnectionName());
        } else {
            wConnectionField.setText(connections[0]);
        }
    }

    @Override
    public Shell getShell() {
        return this.shell;
    }

    @Override
    public Button getWbShowFiles() {
        return this.wbShowFiles;
    }

    @Override
    public Class<?> getPackage() {
        return PKG;
    }

    @Override
    public Label getLblLoadingInfo() {
        return this.lblLoadingInfo;
    }

    @Override
    public MS365GetFileNamesMeta getMeta() {
        return this.ms365GetFileNamesMeta;
    }

    @Override
    public void setSelectedEntity(BaseEntity baseEntity) {

    }

    @Override
    public String[] getConnections() {
        return this.connections;
    }

    @Override
    public TextVar getFilename() {
        return this.wFilename;
    }

    @Override
    public TableView getwFilenameList() {
        return this.wFilenameList;
    }

    @Override
    public TextVar getwFilemask() {
        return this.wFilemask;
    }

    @Override
    public TextVar getwExcludeFilemask() {
        return this.wExcludeFilemask;
    }

    @Override
    public CCombo getwConnectionField() {
        return this.wConnectionField;
    }

    @Override
    public void populateGenericMeta(MS365GetFileNamesMeta ms365GetFileNamesMeta) {
        int nrfiles = this.wFilenameList.getItemCount();
        ms365GetFileNamesMeta.allocate(nrfiles);
        ms365GetFileNamesMeta.setFileName(this.wFilenameList.getItems(0));
        ms365GetFileNamesMeta.setFileMask(this.wFilenameList.getItems(1));
        ms365GetFileNamesMeta.setExcludeFileMask(this.wFilenameList.getItems(2));
        ms365GetFileNamesMeta.setFileRequired(this.wFilenameList.getItems(3));
        ms365GetFileNamesMeta.setIncludeRowNumber(this.wbInclRownum.getSelection());
        ms365GetFileNamesMeta.setRowNumberField(this.wInclRownumField.getText());
        ms365GetFileNamesMeta.setFileField(this.wFileField.getSelection());
        ms365GetFileNamesMeta.setDynamicFilenameField(this.wFilenameField.getText());
        ms365GetFileNamesMeta.setDynamicExcludeWildcardField(this.wExcludeWildcardField.getText());
        ms365GetFileNamesMeta.setDynamicWildcardField(this.wWildcardField.getText());
        ms365GetFileNamesMeta.setRowLimit(Const.toLong(this.wLimit.getText(), 0L));
    }

    private void preview(){
        MS365GetFileNamesMeta ms365GetFileNamesMeta = new MS365GetFileNamesMeta();
        this.populateGenericMeta(ms365GetFileNamesMeta);
        ms365GetFileNamesMeta.setConnectionName(wConnectionField.getText());

        TransMeta previewMeta = TransPreviewFactory.generatePreviewTransformation(this.transMeta, ms365GetFileNamesMeta, this.wStepname.getText());
        EnterNumberDialog numberDialog = new EnterNumberDialog(this.shell, this.props.getDefaultPreviewSize(), BaseMessages.getString(PKG, "MS365GetFileNamesDialog.PreviewSize.DialogTitle", new String[0]), BaseMessages.getString(PKG, "MS365GetFileNamesDialog.PreviewSize.DialogMessage", new String[0]));
        int previewSize = numberDialog.open();
        if (previewSize > 0) {
            TransPreviewProgressDialog progressDialog = new TransPreviewProgressDialog(this.shell, previewMeta, new String[]{this.wStepname.getText()}, new int[]{previewSize});
            progressDialog.open();
            if (!progressDialog.isCancelled()) {
                Trans trans = progressDialog.getTrans();
                String loggingText = progressDialog.getLoggingText();
                if (trans.getResult() != null && trans.getResult().getNrErrors() > 0L) {
                    EnterTextDialog etd = new EnterTextDialog(this.shell, BaseMessages.getString(PKG, "System.Dialog.Error.Title", new String[0]), BaseMessages.getString(PKG, "MS365GetFileNamesDialog.ErrorInPreview.DialogMessage", new String[0]), loggingText, true);
                    etd.setReadOnly();
                    etd.open();
                }

                PreviewRowsDialog prd = new PreviewRowsDialog(this.shell, this.transMeta, 0, this.wStepname.getText(), progressDialog.getPreviewRowsMeta(this.wStepname.getText()), progressDialog.getPreviewRows(this.wStepname.getText()), loggingText);
                prd.open();
            }
        }
    }
}
