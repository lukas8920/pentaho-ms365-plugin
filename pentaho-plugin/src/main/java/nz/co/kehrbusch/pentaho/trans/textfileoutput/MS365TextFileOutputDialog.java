package nz.co.kehrbusch.pentaho.trans.textfileoutput;

import nz.co.kehrbusch.pentaho.connections.manage.ConnectionDetailsInterface;
import nz.co.kehrbusch.pentaho.connections.manage.GraphConnectionDetails;
import nz.co.kehrbusch.pentaho.connections.manage.MS365ConnectionManager;
import nz.co.kehrbusch.pentaho.util.ms365opensavedialog.MS365OpenSaveDialog;
import nz.co.kehrbusch.pentaho.util.ms365opensavedialog.providers.MS365File;
import nz.co.kehrbusch.pentaho.util.ms365opensavedialog.providers.MS365FileProvider;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CCombo;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.widgets.*;
import org.pentaho.di.core.Const;
import org.pentaho.di.core.logging.LogChannel;
import org.pentaho.di.core.util.Utils;
import org.pentaho.di.i18n.BaseMessages;
import org.pentaho.di.plugins.fileopensave.service.ProviderServiceService;
import org.pentaho.di.trans.TransMeta;
import org.pentaho.di.ui.core.FileDialogOperation;
import org.pentaho.di.ui.core.widget.TextVar;
import org.pentaho.di.ui.trans.steps.textfileoutput.TextFileOutputDialog;

import java.util.Arrays;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class MS365TextFileOutputDialog extends TextFileOutputDialog {
    private static final Class<?> PKG = MS365TextFileOutputDialog.class;
    private static final int WIDTH = ( Const.isOSX() || Const.isLinux() ) ? 930 : 947;
    private static final int HEIGHT = ( Const.isOSX() || Const.isLinux() ) ? 618 : 626;

    private boolean isInitialized = false;

    private final MS365TextFileOutputMeta ms365TextFileOutputMeta;
    private final MS365ConnectionManager connectionManager;

    private String[] connections;
    private CCombo wConnectionField;
    private Button wbFilename;
    private Button wFieldNameInField;
    private MS365File selectedFile;
    private TextVar wFilename;
    private TextVar wExtension;

    public MS365TextFileOutputDialog(Shell parent, Object in, TransMeta transMeta, String sname) {
        super(parent, in, transMeta, sname);
        this.ms365TextFileOutputMeta = (MS365TextFileOutputMeta) in;
        this.connectionManager = MS365ConnectionManager.getInstance(new LogChannel());
    }

    public String open(){
        this.ms365TextFileOutputMeta.setMS365TextFileOutputDialog(this);
        return super.open();
    }

    private void addControls(){
        Control[] controls = this.shell.getChildren();
        Control[] wTabFolder = ((CTabFolder) Arrays.stream(controls).filter(control -> control instanceof CTabFolder).collect(Collectors.toList()).get(0)).getChildren();
        Composite wFileComp = ((Composite) Arrays.stream(wTabFolder).filter(control -> control instanceof Composite).collect(Collectors.toList()).get(1));
        Control[] fileChildren = wFileComp.getChildren();

        int margin = 4;
        int middle = this.props.getMiddlePct();

        Label wlFilename = (Label) Arrays.stream(fileChildren).filter(control -> control instanceof Label).collect(Collectors.toList()).get(0);
        wFilename = (TextVar) Arrays.stream(fileChildren).filter(control -> control instanceof TextVar).collect(Collectors.toList()).get(0);
        wbFilename = (Button) Arrays.stream(fileChildren).filter(control -> control instanceof Button).collect(Collectors.toList()).get(0);
        Label wlFileNameField = (Label) Arrays.stream(fileChildren).filter(control -> control instanceof Label && ((Label) control).getText().equals(BaseMessages.getString(PKG, "MS365TextFileOutputDialog.FileNameField.Label", new String[0]))).collect(Collectors.toList()).get(0);
        wFieldNameInField = (Button) ((FormData) wlFileNameField.getLayoutData()).top.control;
        wExtension = (TextVar) Arrays.stream(fileChildren).filter(control -> control instanceof TextVar).collect(Collectors.toList()).get(1);
        Label wlAddToResult = (Label) Arrays.stream(fileChildren).filter(control -> control instanceof Label && ((Label) control).getText().equals(BaseMessages.getString(PKG, "MS365TextFileOutputDialog.AddFileToResult.Label", new String[0]))).findFirst().get();;
        Button wAddToResult = (Button) Arrays.stream(fileChildren).filter(control -> control instanceof Button && control.getToolTipText() != null && ((Button) control).getToolTipText().equals(BaseMessages.getString(PKG, "MS365TextFileOutputDialog.AddFileToResult.Tooltip", new String[0]))).findFirst().get();
        Label wlServletOutput = (Label) Arrays.stream(fileChildren).filter(control -> control instanceof Label && ((Label) control).getText().equals(BaseMessages.getString(PKG, "MS365TextFileOutputDialog.ServletOutput.Label", new String[0]))).findFirst().get();;
        Button wServletOutput = (Button) Arrays.stream(fileChildren).filter(control -> control instanceof Button && control.getToolTipText() != null && ((Button) control).getToolTipText().equals(BaseMessages.getString(PKG, "MS365TextFileOutputDialog.ServletOutput.Tooltip", new String[0]))).findFirst().get();
        Label wlCreateParentFolder = (Label) Arrays.stream(fileChildren).filter(control -> control instanceof Label && ((Label) control).getText().equals(BaseMessages.getString(PKG, "MS365TextFileOutputDialog.CreateParentFolder.Label", new String[0]))).findFirst().get();;
        Button wCreateParentFolder = (Button) Arrays.stream(fileChildren).filter(control -> control instanceof Button && control.getToolTipText() != null && ((Button) control).getToolTipText().equals(BaseMessages.getString(PKG, "MS365TextFileOutputDialog.CreateParentFolder.Tooltip", new String[0]))).findFirst().get();

        Label wlConnection = new Label(wFileComp, 131072);
        wlConnection.setText(BaseMessages.getString(PKG, "MS365TextFileOutputDialog.Connection.Label"));
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
            this.ms365TextFileOutputMeta.setChanged();
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
        fdlFilename = (FormData) wbFilename.getLayoutData();
        fdlFilename.top = new FormAttachment(wConnectionField, margin);
        fdlFilename.right = new FormAttachment(100, 0);
        wbFilename.setVisible(false);
        wbFilename = new Button(wFileComp, 16777224);
        this.props.setLook(wbFilename);
        wbFilename.setLayoutData(fdlFilename);
        wbFilename.setText(BaseMessages.getString(PKG, "System.Button.Browse", new String[0]));
        wbFilename.addSelectionListener(this.saveSelectionListener);
        fdlFilename = (FormData) wFilename.getLayoutData();
        fdlFilename.top = new FormAttachment(wConnectionField, margin);
        fdlFilename.right = new FormAttachment(wbFilename, -margin);
        wFilename.setLayoutData(fdlFilename);

        wFieldNameInField.addSelectionListener(this.activeFieldSelection);

        wlAddToResult.dispose();
        //keep, but hide, Button to avoid NullPointerExceptions
        wAddToResult.setVisible(false);

        wlServletOutput.dispose();
        //keep, but hide, Button to avoid NullPointerExceptions
        wServletOutput.setVisible(false);

        FormData fdlCreateParent = (FormData) wlCreateParentFolder.getLayoutData();
        fdlCreateParent.top = new FormAttachment(wFilename, margin);
        wlCreateParentFolder.setLayoutData(fdlCreateParent);
        fdlCreateParent = (FormData) wCreateParentFolder.getLayoutData();
        fdlCreateParent.top = new FormAttachment(wFilename, margin);
        wCreateParentFolder.setLayoutData(fdlCreateParent);

        wFileComp.layout();

        Listener[] listeners = this.wOK.getListeners(13);
        Listener overwriteListener = event -> {
            MS365TextFileOutputDialog.this.ok();
            listeners[0].handleEvent(event);
        };
        this.wOK.removeListener(13, listeners[0]);
        this.wOK.addListener(13, overwriteListener);

        this.populateDialog();
    }

    private void ok(){
        if (!Utils.isEmpty(this.wStepname.getText())) {
            this.ms365TextFileOutputMeta.setConnectionName(this.wConnectionField.getText());
        }
    }

    private void populateDialog(){
        if (ms365TextFileOutputMeta.getConnectionName() != null){
            wConnectionField.setText(ms365TextFileOutputMeta.getConnectionName());
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

    SelectionListener activeFieldSelection = new SelectionListener() {
        @Override
        public void widgetSelected(SelectionEvent selectionEvent) {
            MS365TextFileOutputDialog.this.wbFilename.setEnabled(!MS365TextFileOutputDialog.this.wFieldNameInField.getSelection());
        }

        @Override
        public void widgetDefaultSelected(SelectionEvent selectionEvent) {

        }
    };

    SelectionListener saveSelectionListener = new SelectionListener() {
        @Override
        public void widgetSelected(SelectionEvent selectionEvent) {
            if (connections.length > 0 && wConnectionField.getText().length() > 0){
                GraphConnectionDetails graphConnectionDetails = (GraphConnectionDetails) connectionManager.provideDetailsByConnectionName(wConnectionField.getText());

                MS365OpenSaveDialog ms365OpenSaveDialog = new MS365OpenSaveDialog(MS365TextFileOutputDialog.this.shell, WIDTH, HEIGHT, new LogChannel());
                ProviderServiceService.get()
                        .add(new MS365FileProvider(graphConnectionDetails.getISharepointConnection(),
                                () -> ms365OpenSaveDialog.refreshDisplay(false),
                                ms365OpenSaveDialog::setLoadingVisibility));
                FileDialogOperation fileDialogOperation = new FileDialogOperation(FileDialogOperation.SAVE_AS);
                fileDialogOperation.setProvider(MS365FileProvider.TYPE);

                ms365OpenSaveDialog.setProviderFilter(MS365FileProvider.TYPE);
                ms365OpenSaveDialog.setProvider(MS365FileProvider.TYPE);
                ms365OpenSaveDialog.open(fileDialogOperation);

                if (ms365OpenSaveDialog.getSelectedFile() != null && ms365OpenSaveDialog.getSelectedFile() instanceof MS365File){
                    MS365File selectedFile = (MS365File) ms365OpenSaveDialog.getSelectedFile();
                    if (selectedFile != null){
                        MS365TextFileOutputDialog.this.selectedFile = selectedFile;
                        MS365TextFileOutputDialog.this.wGet.setVisible(true);
                        String[] type = MS365TextFileOutputDialog.this.selectedFile.getName().split("\\.");
                        if (type.length > 1){
                            MS365TextFileOutputDialog.this.wFilename.setText(MS365TextFileOutputDialog.this.selectedFile.getPath() + type[0]);
                            MS365TextFileOutputDialog.this.wExtension.setText(type[1]);
                        } else {
                            MS365TextFileOutputDialog.this.wFilename.setText(MS365TextFileOutputDialog.this.selectedFile.getPath() + MS365TextFileOutputDialog.this.selectedFile.getName());
                        }
                    }
                } else if (ms365OpenSaveDialog.getSelectedFile() != null){
                    MS365TextFileOutputDialog.this.wFilename.setText(ms365OpenSaveDialog.getSelectedFile().getPath() + ms365OpenSaveDialog.getSelectedFile().getName());
                }
            } else if (!(connections.length > 0)){
                MessageBox mb = new MessageBox(shell, SWT.OK | SWT.ICON_ERROR);
                mb.setMessage(BaseMessages.getString(PKG, "MS365TextFileOutputDialog.NoConnectionCreated.Error"));
                mb.open();
            } else {
                MessageBox mb = new MessageBox(shell, SWT.OK | SWT.ICON_ERROR);
                mb.setMessage(BaseMessages.getString(PKG, "MS365TextFileOutputDialog.NoConnectionChosen.Error"));
                mb.open();
            }
        }

        @Override
        public void widgetDefaultSelected(SelectionEvent selectionEvent) {

        }
    };
}
