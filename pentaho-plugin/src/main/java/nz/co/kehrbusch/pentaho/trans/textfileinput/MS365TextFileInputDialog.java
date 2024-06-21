package nz.co.kehrbusch.pentaho.trans.textfileinput;

import nz.co.kehrbusch.pentaho.connections.manage.ConnectionDetailsInterface;
import nz.co.kehrbusch.pentaho.connections.manage.MS365ConnectionManager;
import org.eclipse.swt.custom.CCombo;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.widgets.*;
import org.pentaho.di.core.logging.LogChannel;
import org.pentaho.di.i18n.BaseMessages;
import org.pentaho.di.trans.TransMeta;
import org.pentaho.di.ui.core.widget.TextVar;
import org.pentaho.di.ui.trans.steps.fileinput.text.TextFileInputDialog;

import java.util.Arrays;
import java.util.stream.Collectors;

public class MS365TextFileInputDialog extends TextFileInputDialog {
    private static final Class<?> PKG = MS365TextFileInputDialog.class;

    private boolean isInitialized = false;

    private MS365TextFileInputMeta ms365TextFileInputMeta;
    private MS365ConnectionManager connectionManager;

    private CCombo wConnectionField;

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

        int margin = 4;
        int middle = this.props.getMiddlePct();

        Label wlFilename = (Label) Arrays.stream(fileChildren).filter(control -> control instanceof Label).collect(Collectors.toList()).get(0);;
        TextVar wFilename = (TextVar) Arrays.stream(fileChildren).filter(control -> control instanceof TextVar).collect(Collectors.toList()).get(0);
        Button wbbFilename = (Button) Arrays.stream(fileChildren).filter(control -> control instanceof Button).collect(Collectors.toList()).get(0);
        Button wbaFilename = (Button) Arrays.stream(fileChildren).filter(control -> control instanceof Button).collect(Collectors.toList()).get(1);

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
        //wbbFilename.addSelectionListener(this.selectSelectionListener);
        fdlFilename = (FormData) wbaFilename.getLayoutData();
        fdlFilename.top = new FormAttachment(wConnectionField, margin);
        fdlFilename.right = new FormAttachment(wbbFilename, -margin);
        wbaFilename.setLayoutData(fdlFilename);
        fdlFilename = (FormData) wFilename.getLayoutData();
        fdlFilename.left = new FormAttachment(middle, 0);
        fdlFilename.top = new FormAttachment(wConnectionField, margin);
        fdlFilename.right = new FormAttachment(wbaFilename, -margin);
        wFilename.setLayoutData(fdlFilename);


        wFileComp.layout();

        this.populateDialog();
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
}
