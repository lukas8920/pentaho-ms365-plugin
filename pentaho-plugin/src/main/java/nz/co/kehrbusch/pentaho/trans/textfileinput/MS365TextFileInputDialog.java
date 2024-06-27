package nz.co.kehrbusch.pentaho.trans.textfileinput;

import nz.co.kehrbusch.ms365.interfaces.ISharepointConnection;
import nz.co.kehrbusch.ms365.interfaces.entities.IStreamProvider;
import nz.co.kehrbusch.pentaho.connections.manage.ConnectionDetailsInterface;
import nz.co.kehrbusch.pentaho.connections.manage.GraphConnectionDetails;
import nz.co.kehrbusch.pentaho.connections.manage.MS365ConnectionManager;
import nz.co.kehrbusch.pentaho.util.file.SharepointFileWrapper;
import nz.co.kehrbusch.pentaho.util.listeners.*;
import nz.co.kehrbusch.pentaho.util.ms365opensavedialog.providers.*;
import org.eclipse.swt.custom.CCombo;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.widgets.*;
import org.pentaho.di.core.Const;
import org.pentaho.di.core.compress.CompressionInputStream;
import org.pentaho.di.core.compress.CompressionProvider;
import org.pentaho.di.core.compress.CompressionProviderFactory;
import org.pentaho.di.core.exception.KettleException;
import org.pentaho.di.core.logging.LogChannel;
import org.pentaho.di.core.util.Utils;
import org.pentaho.di.i18n.BaseMessages;
import org.pentaho.di.trans.Trans;
import org.pentaho.di.trans.TransMeta;
import org.pentaho.di.trans.TransPreviewFactory;
import org.pentaho.di.trans.steps.common.CsvInputAwareMeta;
import org.pentaho.di.trans.steps.fileinput.text.BufferedInputStreamReader;
import org.pentaho.di.trans.steps.fileinput.text.EncodingType;
import org.pentaho.di.trans.steps.fileinput.text.TextFileInputMeta;
import org.pentaho.di.trans.steps.fileinput.text.TextFileInputUtils;
import org.pentaho.di.ui.core.dialog.*;
import org.pentaho.di.ui.core.widget.ColumnInfo;
import org.pentaho.di.ui.core.widget.TableView;
import org.pentaho.di.ui.core.widget.TextVar;
import org.pentaho.di.ui.trans.dialog.TransPreviewProgressDialog;
import org.pentaho.di.ui.trans.steps.fileinput.text.TextFileInputDialog;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

public class MS365TextFileInputDialog extends TextFileInputDialog implements LsSelectFileFolder.FileFolderInput, LsWbaSelection.WbaSelectionInput, LsShowFiles.ShowFilesInput<MS365TextFileInputMeta> {
    private static final Class<?> PKG = MS365TextFileInputDialog.class;

    private boolean isInitialized = false;

    private final MS365TextFileInputMeta ms365TextFileInputMeta;
    private final MS365ConnectionManager connectionManager;
    private BaseEntity selectedEntity;

    private CCombo wConnectionField;
    private TextVar wFilename;
    private TableView wFilenameList;
    private TextVar wFilemask;
    private TextVar wExcludeFilemask;
    private Label lblLoadingInfo;
    private Button wbFirst;
    private Button wbFirstHeader;
    private Button wbShowFiles;

    private String[] connections;

    public MS365TextFileInputDialog(Shell parent, Object in, TransMeta transMeta, String sname) {
        super(parent, in, transMeta, sname);
        this.ms365TextFileInputMeta = (MS365TextFileInputMeta) in;
        this.connectionManager = MS365ConnectionManager.getInstance(new LogChannel());
    }

    @Override
    public String open(){
        ((MS365TextFileInputMeta) MS365TextFileInputDialog.this.ms365TextFileInputMeta).setMs365TextFileInputDialog(this);
        return super.open();
    }

    private void addControls(){
        Control[] controls = this.shell.getChildren();
        Control[] wTabFolder = ((CTabFolder) Arrays.stream(controls).filter(control -> control instanceof CTabFolder).collect(Collectors.toList()).get(0)).getChildren();
        Control[] wFileSComp = ((ScrolledComposite) Arrays.stream(wTabFolder).filter(control -> control instanceof ScrolledComposite).collect(Collectors.toList()).get(0)).getChildren();
        Composite wFileComp = ((Composite) Arrays.stream(wFileSComp).filter(control -> control instanceof Composite).collect(Collectors.toList()).get(0));
        Control[] fileChildren = wFileComp.getChildren();
        Control[] wContentSComp = ((ScrolledComposite) Arrays.stream(wTabFolder).filter(control -> control instanceof ScrolledComposite).collect(Collectors.toList()).get(1)).getChildren();
        Composite wContentComp = ((Composite) Arrays.stream(wContentSComp).filter(control -> control instanceof Composite).collect(Collectors.toList()).get(0));
        Control[] contentChildren = wContentComp.getChildren();

        int margin = 4;
        int middle = this.props.getMiddlePct();

        Label wlFilename = (Label) Arrays.stream(fileChildren).filter(control -> control instanceof Label).collect(Collectors.toList()).get(0);
        wFilename = (TextVar) Arrays.stream(fileChildren).filter(control -> control instanceof TextVar).collect(Collectors.toList()).get(0);
        Button wbbFilename = (Button) Arrays.stream(fileChildren).filter(control -> control instanceof Button).collect(Collectors.toList()).get(0);
        Button wbaFilename = (Button) Arrays.stream(fileChildren).filter(control -> control instanceof Button).collect(Collectors.toList()).get(1);
        wFilenameList = (TableView) Arrays.stream(fileChildren).filter(control -> control instanceof TableView).findFirst().orElse(null);
        Label wlFiletype = (Label) Arrays.stream(contentChildren).filter(control -> control instanceof Label).collect(Collectors.toList()).get(0);
        CCombo wFiletype = (CCombo) Arrays.stream(contentChildren).filter(control -> control instanceof CCombo).collect(Collectors.toList()).get(0);
        Label wlSeparator = (Label) Arrays.stream(contentChildren).filter(control -> control instanceof Label).collect(Collectors.toList()).get(1);
        TextVar wSeparator = (TextVar) Arrays.stream(contentChildren).filter(control -> control instanceof TextVar).collect(Collectors.toList()).get(0);
        Button wbSeparator = (Button) Arrays.stream(contentChildren).filter(control -> control instanceof Button).collect(Collectors.toList()).get(0);
        wFilemask = (TextVar) Arrays.stream(fileChildren).filter(control -> control instanceof TextVar).collect(Collectors.toList()).get(1);
        wExcludeFilemask = (TextVar) Arrays.stream(fileChildren).filter(control -> control instanceof TextVar).collect(Collectors.toList()).get(2);
        wbFirst = (Button) Arrays.stream(fileChildren).filter(control -> control instanceof Button && ((Button) control).getText().equals(BaseMessages.getString(PKG, "MS365TextFileInputDialog.First.Button", new String[0]))).collect(Collectors.toList()).get(0);;
        wbFirstHeader = (Button) Arrays.stream(fileChildren).filter(control -> control instanceof Button && ((Button) control).getText().equals(BaseMessages.getString(PKG, "MS365TextFileInputDialog.FirstHeader.Button", new String[0]))).collect(Collectors.toList()).get(0);
        wbShowFiles = (Button) Arrays.stream(fileChildren).filter(control -> control instanceof Button && ((Button) control).getText().equals(BaseMessages.getString(PKG, "MS365TextFileInputDialog.ShowFiles.Button", new String[0]))).collect(Collectors.toList()).get(0);

        this.wPreview.removeListener(13, this.lsPreview);
        this.lsPreview = event -> MS365TextFileInputDialog.this.preview();
        this.wPreview.addListener(13, this.lsPreview);

        this.wGet.setVisible(false);

        Label wlConnection = new Label(wFileComp, 131072);
        wlConnection.setText(BaseMessages.getString(PKG, "MS365TextFileInputDialog.Connection.Label"));
        FormData fdlConnection = new FormData();
        fdlConnection.top = new FormAttachment(0, margin);
        fdlConnection.left = new FormAttachment(0, 0);
        fdlConnection.right = new FormAttachment(middle, -margin);
        this.props.setLook(wlConnection);
        wlConnection.setLayoutData(fdlConnection);
        wConnectionField = new CCombo(wFileComp, 18436);
        connections = connectionManager.getConnections().stream().map(ConnectionDetailsInterface::getConnectionName).toArray(String[]::new);
        wConnectionField.setItems(connections);
        this.props.setLook(wConnectionField);
        wConnectionField.addModifyListener(modifyEvent -> {
            this.ms365TextFileInputMeta.setChanged();
        });
        fdlConnection = new FormData();
        fdlConnection.top = new FormAttachment(0, margin);
        fdlConnection.left = new FormAttachment(middle, 0);
        fdlConnection.right = new FormAttachment(100, 0);
        this.props.setLook(wConnectionField);
        wConnectionField.setLayoutData(fdlConnection);

        FormData fdlFilename = (FormData) wlFilename.getLayoutData();
        fdlFilename.top = new FormAttachment(wConnectionField, margin);
        wlFilename.setLayoutData(fdlFilename);
        fdlFilename = (FormData) wbbFilename.getLayoutData();
        fdlFilename.top = new FormAttachment(wConnectionField, margin);
        fdlFilename.right = new FormAttachment(100, 0);
        wbbFilename.setVisible(false);
        wbbFilename = new Button(wFileComp, 16777224);
        this.props.setLook(wbbFilename);
        wbbFilename.setLayoutData(fdlFilename);
        wbbFilename.setText(BaseMessages.getString(PKG, "System.Button.Browse", new String[0]));
        wbbFilename.addSelectionListener(new LsSelectFileFolder(this));

        fdlFilename = (FormData) wbaFilename.getLayoutData();
        wbaFilename.setVisible(false);
        wbaFilename = new Button(wFileComp, 16777224);
        this.props.setLook(wbaFilename);
        wbaFilename.setText(BaseMessages.getString(PKG, "MS365TextFileInputDialog.FilenameAdd.Button", new String[0]));
        wbaFilename.setToolTipText(BaseMessages.getString(PKG, "MS365TextFileInputDialog.FilenameAdd.Tooltip", new String[0]));
        fdlFilename.top = new FormAttachment(wConnectionField, margin);
        fdlFilename.right = new FormAttachment(wbbFilename, -margin);
        wbaFilename.setLayoutData(fdlFilename);
        wbaFilename.addSelectionListener(new LsWbaSelection(this));
        fdlFilename = (FormData) wFilename.getLayoutData();
        fdlFilename.left = new FormAttachment(middle, 0);
        fdlFilename.top = new FormAttachment(wConnectionField, margin);
        fdlFilename.right = new FormAttachment(wbaFilename, -margin);
        wFilename.setLayoutData(fdlFilename);

        Listener[] listeners = this.wOK.getListeners(13);
        Listener overwriteListener = event -> {
            MS365TextFileInputDialog.this.ok();
            listeners[0].handleEvent(event);
        };
        this.wOK.removeListener(13, listeners[0]);
        this.wOK.addListener(13, overwriteListener);

        wlFiletype.dispose();
        wFiletype.setVisible(false);

        FormData fbdFiletype = (FormData) wlSeparator.getLayoutData();
        fbdFiletype.top = new FormAttachment(0, 0);
        wlSeparator.setLayoutData(fbdFiletype);
        fbdFiletype = (FormData) wSeparator.getLayoutData();
        fbdFiletype.top = new FormAttachment(0, 0);
        wSeparator.setLayoutData(fbdFiletype);
        fbdFiletype = (FormData) wbSeparator.getLayoutData();
        fbdFiletype.top = new FormAttachment(0, 0);
        wbSeparator.setLayoutData(fbdFiletype);

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

        lblLoadingInfo = new Label(this.shell, 131072);
        lblLoadingInfo.setText(BaseMessages.getString(PKG, "MS365CsvInput.Loading.Label"));
        props.setLook(lblLoadingInfo);
        FormData fbdLoading = new FormData();
        fbdLoading.bottom = new FormAttachment(100, -11);
        fbdLoading.left = new FormAttachment(wCancel, 80);
        lblLoadingInfo.setLayoutData(fbdLoading);
        lblLoadingInfo.setVisible(false);

        Listener[] firstListeners = wbFirst.getListeners(13);
        wbFirst.removeListener(13, firstListeners[0]);
        wbFirst.addListener(13, new LsFirst(this));

        Listener[] firstHeaderListeners = wbFirstHeader.getListeners(13);
        wbFirstHeader.removeListener(13, firstHeaderListeners[0]);
        wbFirstHeader.addListener(13, new LsFirstHeader(this));

        Listener[] showFilesListeners = wbShowFiles.getListeners(13);
        wbShowFiles.removeListener(13, showFilesListeners[0]);
        wbShowFiles.addListener(13, new LsShowFiles(this));

        wFileComp.layout();

        this.populateDialog();
    }

    private void ok(){
        if (!Utils.isEmpty(this.wStepname.getText())) {
            this.ms365TextFileInputMeta.setConnectionName(this.wConnectionField.getText());
        }
    }

    private void populateDialog(){
        if (ms365TextFileInputMeta.getConnectionName() != null){
            wConnectionField.setText(ms365TextFileInputMeta.getConnectionName());
        } else {
            wConnectionField.setText(connections[0]);
        }
    }

    public void attachAdditionalFields() {
        if (!this.isInitialized){
            addControls();
            this.isInitialized = true;
        }
    }

    @Override
    public InputStream getInputStream(CsvInputAwareMeta meta) {
        InputStream inputStream = new ByteArrayInputStream(new byte[0]);
        if (this.selectedEntity instanceof MS365File){
            IStreamProvider iStreamProvider = (MS365File) this.selectedEntity;

            try {
                GraphConnectionDetails graphConnectionDetails = (GraphConnectionDetails) this.connectionManager.provideDetailsByConnectionName(this.wConnectionField.getText());
                inputStream = iStreamProvider.getInputStream(graphConnectionDetails.getISharepointConnection());
                CompressionProvider provider = CompressionProviderFactory.getInstance().createCompressionProviderInstance(((TextFileInputMeta)meta).content.fileCompression);
                inputStream = provider.createInputStream(inputStream);
            } catch (Exception var6) {
                this.logError("No input stream could be received from the server.", var6);
            }

            return inputStream;
        } else {
            this.logError("No input stream can be received from a directory");
        }
        return inputStream;
    }

    private void preview() {
        MS365TextFileInputMeta oneMeta = new MS365TextFileInputMeta();
        super.populateMeta(oneMeta);
        oneMeta.setConnectionName(wConnectionField.getText());

        if (oneMeta.inputFiles.acceptingFilenames) {
            MessageBox mb = new MessageBox(this.shell, 34);
            mb.setMessage(BaseMessages.getString(PKG, "MS365TextFileInputDialog.Dialog.SpecifyASampleFile.Message", new String[0]));
            mb.setText(BaseMessages.getString(PKG, "MS365TextFileInputDialog.Dialog.SpecifyASampleFile.Title", new String[0]));
            mb.open();
        } else {
            TransMeta previewMeta = TransPreviewFactory.generatePreviewTransformation(this.transMeta, oneMeta, this.wStepname.getText());
            EnterNumberDialog numberDialog = new EnterNumberDialog(this.shell, this.props.getDefaultPreviewSize(), BaseMessages.getString(PKG, "MS365TextFileInputDialog.PreviewSize.DialogTitle", new String[0]), BaseMessages.getString(PKG, "MS365TextFileInputDialog.PreviewSize.DialogMessage", new String[0]));
            int previewSize = numberDialog.open();
            if (previewSize > 0) {
                TransPreviewProgressDialog progressDialog = new TransPreviewProgressDialog(this.shell, previewMeta, new String[]{this.wStepname.getText()}, new int[]{previewSize});
                progressDialog.open();
                Trans trans = progressDialog.getTrans();
                String loggingText = progressDialog.getLoggingText();
                if (!progressDialog.isCancelled() && trans.getResult() != null && trans.getResult().getNrErrors() > 0L) {
                    EnterTextDialog etd = new EnterTextDialog(this.shell, BaseMessages.getString(PKG, "System.Dialog.PreviewError.Title", new String[0]), BaseMessages.getString(PKG, "System.Dialog.PreviewError.Message", new String[0]), loggingText, true);
                    etd.setReadOnly();
                    etd.open();
                }

                PreviewRowsDialog prd = new PreviewRowsDialog(this.shell, this.transMeta, 0, this.wStepname.getText(), progressDialog.getPreviewRowsMeta(this.wStepname.getText()), progressDialog.getPreviewRows(this.wStepname.getText()), loggingText);
                prd.open();
            }

        }
    }

    public void first(ISharepointConnection iSharepointConnection, SharepointFileWrapper wrapper, boolean skipHeaders) {
        MS365TextFileInputMeta info = new MS365TextFileInputMeta();
        super.populateMeta(info);

        try {
            if (wrapper.getStreamProviders().size() > 0) {
                String shellText = BaseMessages.getString(PKG, "MS365TextFileInputDialog.LinesToView.DialogTitle", new String[0]);
                String lineText = BaseMessages.getString(PKG, "MS365TextFileInputDialog.LinesToView.DialogMessage", new String[0]);
                EnterNumberDialog end = new EnterNumberDialog(this.shell, 100, shellText, lineText);
                int nrLines = end.open();
                if (nrLines >= 0) {
                    List<String> linesList = this.getFirst(iSharepointConnection, wrapper, nrLines, skipHeaders);
                    if (linesList != null && linesList.size() > 0) {
                        String firstlines = "";

                        String aLinesList;
                        for(Iterator var9 = linesList.iterator(); var9.hasNext(); firstlines = firstlines + aLinesList + Const.CR) {
                            aLinesList = (String)var9.next();
                        }

                        EnterTextDialog etd = new EnterTextDialog(this.shell, BaseMessages.getString(PKG, "MS365TextFileInputDialog.ContentOfFirstFile.DialogTitle", new String[0]), nrLines == 0 ? BaseMessages.getString(PKG, "MS365TextFileInputDialog.ContentOfFirstFile.AllLines.DialogMessage", new String[0]) : BaseMessages.getString(PKG, "MS365TextFileInputDialog.ContentOfFirstFile.NLines.DialogMessage", new String[]{"" + nrLines}), firstlines, true);
                        etd.setReadOnly();
                        etd.open();
                    } else {
                        MessageBox mb = new MessageBox(this.shell, 33);
                        mb.setMessage(BaseMessages.getString(PKG, "MS365TextFileInputDialog.UnableToReadLines.DialogMessage", new String[0]));
                        mb.setText(BaseMessages.getString(PKG, "MS365TextFileInputDialog.UnableToReadLines.DialogTitle", new String[0]));
                        mb.open();
                    }
                }
            } else {
                MessageBox mb = new MessageBox(this.shell, 33);
                mb.setMessage(BaseMessages.getString(PKG, "MS365TextFileInputDialog.NoValidFile.DialogMessage", new String[0]));
                mb.setText(BaseMessages.getString(PKG, "System.Dialog.Error.Title", new String[0]));
                mb.open();
            }
        } catch (KettleException var11) {
            new ErrorDialog(this.shell, BaseMessages.getString(PKG, "System.Dialog.Error.Title", new String[0]), BaseMessages.getString(PKG, "MS365TextFileInputDialog.ErrorGettingData.DialogMessage", new String[0]), var11);
        }

    }

    private List<String> getFirst(ISharepointConnection iSharepointConnection, SharepointFileWrapper<MS365TextFileInputMeta> wrapper, int nrlines, boolean skipHeaders) throws KettleException {
        MS365TextFileInputMeta meta = new MS365TextFileInputMeta();
        super.populateMeta(meta);


        CompressionInputStream f = null;
        StringBuilder lineStringBuilder = new StringBuilder(256);
        int fileFormatType = meta.getFileFormatTypeNr();
        List<String> retval = new ArrayList();
        if (wrapper.getStreamProviders().size() > 0) {
            IStreamProvider ms365File = wrapper.getStreamProviders().get(0);

            try {
                InputStream inputStream = ms365File.getInputStream(iSharepointConnection);
                CompressionProvider provider = CompressionProviderFactory.getInstance().createCompressionProviderInstance(meta.content.fileCompression);
                f = provider.createInputStream(inputStream);
                BufferedInputStreamReader reader;
                if (meta.getEncoding() != null && meta.getEncoding().length() > 0) {
                    reader = new BufferedInputStreamReader(new InputStreamReader(f, meta.getEncoding()));
                } else {
                    reader = new BufferedInputStreamReader(new InputStreamReader(f));
                }

                EncodingType encodingType = EncodingType.guessEncodingType(reader.getEncoding());
                int linenr = 0;
                int maxnr = nrlines + (meta.content.header ? meta.content.nrHeaderLines : 0);
                if (skipHeaders) {
                    if (meta.content.layoutPaged && meta.content.nrLinesDocHeader > 0) {
                        TextFileInputUtils.skipLines(this.log, reader, encodingType, fileFormatType, lineStringBuilder, meta.content.nrLinesDocHeader - 1, meta.getEnclosure(), meta.getEscapeCharacter(), 0L);
                    }

                    if (meta.content.header && meta.content.nrHeaderLines > 0) {
                        TextFileInputUtils.skipLines(this.log, reader, encodingType, fileFormatType, lineStringBuilder, meta.content.nrHeaderLines - 1, meta.getEnclosure(), meta.getEscapeCharacter(), 0L);
                    }
                }

                for(String line = TextFileInputUtils.getLine(this.log, reader, encodingType, fileFormatType, lineStringBuilder, meta.getEnclosure(), meta.getEscapeCharacter()); line != null && (linenr < maxnr || nrlines == 0); line = TextFileInputUtils.getLine(this.log, reader, encodingType, fileFormatType, lineStringBuilder, meta.getEnclosure(), meta.getEscapeCharacter())) {
                    retval.add(line);
                    ++linenr;
                }
            } catch (Exception var24) {
                throw new KettleException(BaseMessages.getString(PKG, "MS365TextFileInputDialog.Exception.ErrorGettingFirstLines", new String[]{"" + nrlines, ms365File.getName()}), var24);
            } finally {
                try {
                    if (f != null) {
                        f.close();
                    }
                } catch (Exception var23) {
                }

            }
        }

        return retval;
    }

    public Class<?> getPackage(){
        return PKG;
    }

    public Button getWbShowFiles(){
        return this.wbShowFiles;
    }

    public Button getWbFirst(){
        return this.wbFirst;
    }

    public Button getWbFirstHeader(){
        return this.wbFirstHeader;
    }

    public Label getLblLoadingInfo(){
        return this.lblLoadingInfo;
    }

    @Override
    public MS365TextFileInputMeta getMeta() {
        return this.ms365TextFileInputMeta;
    }

    public TableView getwFilenameList(){
        return this.wFilenameList;
    }

    public TextVar getFilename(){
        return this.wFilename;
    }

    public TextVar getwFilemask(){
        return this.wFilemask;
    }

    public TextVar getwExcludeFilemask(){
        return this.wExcludeFilemask;
    }

    public String[] getConnections(){
        return this.connections;
    }

    public CCombo getwConnectionField(){
        return this.wConnectionField;
    }

    @Override
    public void populateGenericMeta(MS365TextFileInputMeta ms365TextFileInputMeta) {
        super.populateMeta(ms365TextFileInputMeta);
    }

    public Button getWGet(){
        return this.wGet;
    }

    public void setSelectedEntity(BaseEntity baseEntity){
        this.selectedEntity = baseEntity;
    }
}
