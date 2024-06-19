package nz.co.kehrbusch.pentaho.trans.csvinput;

import nz.co.kehrbusch.ms365.interfaces.entities.IStreamProvider;
import nz.co.kehrbusch.pentaho.connections.manage.ConnectionDetailsInterface;
import nz.co.kehrbusch.pentaho.connections.manage.GraphConnectionDetails;
import nz.co.kehrbusch.pentaho.connections.manage.MS365ConnectionManager;
import nz.co.kehrbusch.pentaho.util.ms365opensavedialog.MS365OpenSaveDialog;
import nz.co.kehrbusch.pentaho.util.ms365opensavedialog.providers.MS365FileProvider;
import nz.co.kehrbusch.pentaho.util.ms365opensavedialog.providers.MS365File;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CCombo;
import org.eclipse.swt.events.*;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.widgets.*;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Label;
import org.pentaho.di.core.Const;
import org.pentaho.di.core.logging.LogChannel;
import org.pentaho.di.core.util.Utils;
import org.pentaho.di.i18n.BaseMessages;
import org.pentaho.di.plugins.fileopensave.service.ProviderServiceService;
import org.pentaho.di.trans.Trans;
import org.pentaho.di.trans.TransMeta;
import org.pentaho.di.trans.TransPreviewFactory;
import org.pentaho.di.trans.steps.common.CsvInputAwareMeta;
import org.pentaho.di.ui.core.FileDialogOperation;
import org.pentaho.di.ui.core.dialog.EnterNumberDialog;
import org.pentaho.di.ui.core.dialog.EnterTextDialog;
import org.pentaho.di.ui.core.dialog.PreviewRowsDialog;
import org.pentaho.di.ui.core.widget.TextVar;
import org.pentaho.di.ui.trans.dialog.TransPreviewProgressDialog;
import org.pentaho.di.ui.trans.steps.csvinput.CsvInputDialog;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class MS365CsvInputDialog extends CsvInputDialog {
    private static Class<?> PKG = MS365CsvInputDialog.class;
    private static final int WIDTH = ( Const.isOSX() || Const.isLinux() ) ? 930 : 947;
    private static final int HEIGHT = ( Const.isOSX() || Const.isLinux() ) ? 618 : 626;

    private static final Logger log = Logger.getLogger(MS365CsvInputDialog.class.getName());

    private final MS365ConnectionManager connectionManager;
    private final MS365CsvInputMeta ms365CsvInputMeta;

    private TextVar wFilename = null;
    private CCombo wConnectionField;

    private MS365File selectedFile;
    private Label lblLoadingInfo;

    private boolean isInitialized = false;
    private String[] connections;

    public MS365CsvInputDialog(Shell parent, Object in, TransMeta tr, String sname) {
        super(parent, in, tr, sname);
        this.connectionManager = MS365ConnectionManager.getInstance(new LogChannel());
        this.ms365CsvInputMeta = (MS365CsvInputMeta) in;
        this.baseStepMeta = (MS365CsvInputMeta) in;
    }

    public String open(){
        ((MS365CsvInputMeta) MS365CsvInputDialog.this.baseStepMeta).setMs365CsvInputDialog(this);
        return super.open();
    }

    private void addControls(){
        Control[] controls = this.shell.getChildren();
        int margin = 4;
        int middle = this.props.getMiddlePct();
        boolean isReceivingInput = this.transMeta.findNrPrevSteps(this.stepMeta) > 0;

        CCombo cCombo = null;
        Label wlFilename = (Label) Arrays.stream(controls).filter(control -> control instanceof Label).collect(Collectors.toList()).get(1);
        if(isReceivingInput){
            cCombo = (CCombo) Arrays.stream(controls).filter(control -> control instanceof CCombo).collect(Collectors.toList()).get(0);
        } else {
            wFilename = (TextVar) Arrays.stream(controls).filter(control -> control instanceof TextVar).collect(Collectors.toList()).get(0);
            wFilename.addModifyListener(this.modifyListener);
        }

        this.wGet.setVisible(false);
        this.wGet.removeListener(13, super.lsGet);
        this.wGet.addListener(13, lsGet);

        this.wPreview.removeListener(13, this.lsPreview);
        this.lsPreview = new Listener() {
            @Override
            public void handleEvent(Event event) {
                MS365CsvInputDialog.this.preview();
            }
        };
        this.wPreview.addListener(13, this.lsPreview);

        Button wbbFilename = (Button) Arrays.stream(controls).filter(control -> control instanceof Button && ((Button) control).getText().equals(BaseMessages.getString(PKG, "System.Button.Browse", new String[0])))
                .findFirst().orElse(null);
        Label wlAddResult = (Label) Arrays.stream(controls).filter(control -> control instanceof Label && ((Label) control).getText().equals(BaseMessages.getString(PKG, this.ms365CsvInputMeta.getDescription("ADD_FILENAME_RESULT"), new String[0]))).findFirst().orElse(null);
        Button wAddResult = (Button) Arrays.stream(controls).filter(control -> control instanceof Button && control.getToolTipText() != null && control.getToolTipText().equals(BaseMessages.getString(PKG, this.ms365CsvInputMeta.getTooltip("ADD_FILENAME_RESULT"), new String[0]))).findFirst().orElse(null);;
        Label wlRowNumField = (Label) Arrays.stream(controls).filter(control -> control instanceof Label && ((Label) control).getText().equals(BaseMessages.getString(PKG, this.ms365CsvInputMeta.getDescription("ROW_NUM_FIELD"), new String[0]))).findFirst().orElse(null);
        Button wHeaderPresent = (Button) ((FormData) wAddResult.getLayoutData()).top.control;
        Label wlRunningInParallel = (Label) Arrays.stream(controls).filter(control -> control instanceof Label && ((Label) control).getText().equals(BaseMessages.getString(PKG, this.ms365CsvInputMeta.getDescription("PARALLEL"), new String[0]))).findFirst().orElse(null);;
        TextVar wRowNumField = (TextVar) ((FormData) wlRunningInParallel.getLayoutData()).top.control;

        Label wlConnection = new Label(this.shell, 131072);
        wlConnection.setText(BaseMessages.getString(PKG, this.ms365CsvInputMeta.getDescription("CONNECTION_NAME")));
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
            this.ms365CsvInputMeta.setChanged();
        });
        fdlConnection = new FormData();
        fdlConnection.top = new FormAttachment(wStepname, margin);
        fdlConnection.left = new FormAttachment(middle, 0);
        fdlConnection.right = new FormAttachment(100, 0);
        wConnectionField.setLayoutData(fdlConnection);

        if(wbbFilename != null){
            wbbFilename.dispose();
            wbbFilename = new Button(this.shell, 16777224);
            this.props.setLook(wbbFilename);
            wbbFilename.setText(BaseMessages.getString(PKG, "System.Button.Browse", new String[0]));
            wbbFilename.setToolTipText(BaseMessages.getString(PKG, "System.Tooltip.BrowseForFileOrDirAndAdd", new String[0]));
            FormData fdbFilename = new FormData();
            fdbFilename.top = new FormAttachment(wConnectionField, margin);
            fdbFilename.right = new FormAttachment(100, 0);
            wbbFilename.setLayoutData(fdbFilename);
            wbbFilename.addSelectionListener(selectFileListener);
        }


        FormData fdbFilename = new FormData();
        fdbFilename.top = new FormAttachment(wConnectionField, margin);
        fdbFilename.left = new FormAttachment(0, 0);
        fdbFilename.right = new FormAttachment(middle, -margin);
        wlFilename.setLayoutData(fdbFilename);
        if (isReceivingInput){
            FormData formData = (FormData) cCombo.getLayoutData();
            formData.top = new FormAttachment(wConnectionField, margin);
            cCombo.setLayoutData(formData);
        } else {
            FormData formData = (FormData) wFilename.getLayoutData();
            formData.top = new FormAttachment(wConnectionField, margin);
            formData.right = new FormAttachment(wbbFilename, -margin);
            wFilename.setLayoutData(formData);
        }

        Listener[] listeners = this.wOK.getListeners(13);
        Listener overwriteListener = event -> {
              MS365CsvInputDialog.this.ok();
              listeners[0].handleEvent(event);
        };
        this.wOK.removeListener(13, listeners[0]);
        this.wOK.addListener(13, overwriteListener);

        wlAddResult.dispose();
        //keep, but hide, Button to avoid NullPointerExceptions
        wAddResult.setVisible(false);

        lblLoadingInfo = new Label(this.shell, 131072);
        lblLoadingInfo.setText(BaseMessages.getString(PKG, "MS365CsvInput.Loading.Label"));
        props.setLook(lblLoadingInfo);
        FormData fbdLoading = new FormData();
        fbdLoading.bottom = new FormAttachment(100, -7);
        fbdLoading.left = new FormAttachment(wCancel, 80);
        lblLoadingInfo.setLayoutData(fbdLoading);
        lblLoadingInfo.setVisible(false);

        FormData formData = (FormData) wlRowNumField.getLayoutData();
        formData.top = new FormAttachment(wHeaderPresent, margin);
        wlRowNumField.setLayoutData(formData);
        formData = (FormData) wRowNumField.getLayoutData();
        formData.top = new FormAttachment(wHeaderPresent, margin);
        wRowNumField.setLayoutData(formData);

        populateDialog();
    }

    private void ok(){
        if (!Utils.isEmpty(this.wStepname.getText())) {
            this.ms365CsvInputMeta.setConnectionName(this.wConnectionField.getText());
        }
    }

    private void populateDialog(){
        if (ms365CsvInputMeta.getConnectionName() != null){
            wConnectionField.setText(ms365CsvInputMeta.getConnectionName());
        } else {
            wConnectionField.setText(connections[0]);
        }
    }

    public void attachAdditionalFields(){
        if (!this.isInitialized){
            addControls();
            this.isInitialized = true;
        }
    }

    private synchronized void preview(){
        MS365CsvInputMeta oneMeta = new MS365CsvInputMeta();
        super.populateMeta(oneMeta);
        oneMeta.setConnectionName(this.wConnectionField.getText());
        TransMeta previewMeta = TransPreviewFactory.generatePreviewTransformation(this.transMeta, oneMeta, this.wStepname.getText());
        this.transMeta.getVariable("Internal.Transformation.Filename.Directory");
        previewMeta.getVariable("Internal.Transformation.Filename.Directory");
        EnterNumberDialog numberDialog = new EnterNumberDialog(this.shell, this.props.getDefaultPreviewSize(), BaseMessages.getString(PKG, "CsvInputDialog.PreviewSize.DialogTitle", new String[0]), BaseMessages.getString(PKG, "CsvInputDialog.PreviewSize.DialogMessage", new String[0]));
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

    Listener lsGet = event -> {
        this.lblLoadingInfo.setVisible(true);
        super.lsGet.handleEvent(event);
        this.lblLoadingInfo.setVisible(false);
    };

    @Override
    public InputStream getInputStream(CsvInputAwareMeta meta){
        IStreamProvider iStreamProvider = this.selectedFile;
        InputStream inputStream = new ByteArrayInputStream(new byte[0]);

        try {
            GraphConnectionDetails graphConnectionDetails = (GraphConnectionDetails) this.connectionManager.provideDetailsByConnectionName(this.wConnectionField.getText());
            inputStream = iStreamProvider.getInputStream(graphConnectionDetails.getISharepointConnection());
        } catch (IOException e){
            logError("No input stream could be received from the server.");
            return new BufferedInputStream(inputStream);
        }

        return inputStream;
    }

    ModifyListener modifyListener = modifyEvent -> {
        this.wGet.setVisible(this.selectedFile != null &&
                (this.selectedFile.getPath() + this.selectedFile.getName()).equals(this.wFilename.getText()));
    };

    SelectionListener selectFileListener = new SelectionListener() {
        @Override
        public void widgetSelected(SelectionEvent selectionEvent) {
            if (connections.length > 0 && wConnectionField.getText().length() > 0){
                GraphConnectionDetails graphConnectionDetails = (GraphConnectionDetails) connectionManager.provideDetailsByConnectionName(wConnectionField.getText());

                MS365OpenSaveDialog ms365OpenSaveDialog = new MS365OpenSaveDialog(MS365CsvInputDialog.this.shell, WIDTH, HEIGHT, new LogChannel());
                ProviderServiceService.get()
                        .add(new MS365FileProvider(graphConnectionDetails.getISharepointConnection(),
                                () -> ms365OpenSaveDialog.refreshDisplay(false),
                                ms365OpenSaveDialog::setLoadingVisibility));
                FileDialogOperation fileDialogOperation = new FileDialogOperation(FileDialogOperation.SELECT_FILE);
                fileDialogOperation.setProvider(MS365FileProvider.TYPE);

                ms365OpenSaveDialog.setProviderFilter(MS365FileProvider.TYPE);
                ms365OpenSaveDialog.setProvider(MS365FileProvider.TYPE);
                ms365OpenSaveDialog.open(fileDialogOperation);

                if (ms365OpenSaveDialog.getSelectedFile() != null){
                    MS365CsvInputDialog.this.selectedFile = ms365OpenSaveDialog.getSelectedFile();
                    MS365CsvInputDialog.this.wFilename.setText(ms365OpenSaveDialog.getSelectedFile().getPath() + ms365OpenSaveDialog.getSelectedFile().getName());
                    MS365CsvInputDialog.this.wGet.setVisible(true);
                }
            } else if (!(connections.length > 0)){
                MessageBox mb = new MessageBox(shell, SWT.OK | SWT.ICON_ERROR);
                mb.setMessage(BaseMessages.getString(PKG, "MS365CsvInput.NoConnectionCreated.Error"));
                mb.open();
            } else {
                MessageBox mb = new MessageBox(shell, SWT.OK | SWT.ICON_ERROR);
                mb.setMessage(BaseMessages.getString(PKG, "MS365CsvInput.NoConnectionChosen.Error"));
                mb.open();
            }
        }

        @Override
        public void widgetDefaultSelected(SelectionEvent selectionEvent) {

        }
    };

    @Override
    public void dispose(){
        if (this.selectedFile != null){
            try {
                this.selectedFile.disposeInputStream();
            } catch (IOException e) {
                logError("Error while disposing the inputstream for the file");
            }
        }
        super.dispose();
    }
}
