package nz.co.kehrbusch.pentaho.trans.csvinput;

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
import org.pentaho.di.trans.TransMeta;
import org.pentaho.di.ui.core.FileDialogOperation;
import org.pentaho.di.ui.core.widget.TextVar;
import org.pentaho.di.ui.trans.steps.csvinput.CsvInputDialog;

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

    private boolean isInitialized = false;
    private String[] connections;

    public MS365CsvInputDialog(Shell parent, Object in, TransMeta tr, String sname) {
        super(parent, in, tr, sname);
        this.connectionManager = MS365ConnectionManager.getInstance();
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
        }

        Button wbbFilename = (Button) Arrays.stream(controls).filter(control -> control instanceof Button && ((Button) control).getText().equals(BaseMessages.getString(PKG, "System.Button.Browse", new String[0])))
                .findFirst().orElse(null);

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

        fdbFilename = new FormData();
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
}
