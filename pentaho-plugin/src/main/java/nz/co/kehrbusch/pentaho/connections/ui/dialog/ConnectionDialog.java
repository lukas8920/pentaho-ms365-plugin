package nz.co.kehrbusch.pentaho.connections.ui.dialog;

import nz.co.kehrbusch.pentaho.connections.manage.*;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CCombo;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.events.*;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.widgets.*;
import org.pentaho.di.core.Const;
import org.pentaho.di.core.logging.LogChannel;
import org.pentaho.di.i18n.BaseMessages;
import org.pentaho.di.ui.core.PropsUI;
import org.pentaho.di.ui.core.gui.GUIResource;
import org.pentaho.di.ui.core.gui.WindowProperty;
import org.pentaho.di.ui.spoon.Spoon;
import org.pentaho.di.ui.trans.step.BaseStepDialog;
import org.pentaho.di.ui.util.HelpUtils;

import java.util.List;
import java.util.logging.Logger;

import static org.pentaho.di.core.Const.MARGIN;
import static org.pentaho.di.core.util.StringUtil.isEmpty;

public class ConnectionDialog extends Dialog {
    private static final Logger log = Logger.getLogger(ConnectionDialog.class.getName());

    private static final Class<?> PKG = ConnectionDialog.class;
    private static final Image LOGO = GUIResource.getInstance().getImageLogoSmall();

    private static final int TEXT_VAR_FLAGS = SWT.SINGLE | SWT.LEFT | SWT.BORDER;

    private final MS365ConnectionManager connectionManager = MS365ConnectionManager.getInstance(new LogChannel());

    private final int width;
    private final int height;
    private final PropsUI props;
    private final List<ConnectionTypeInterface> connectionTypes;
    private final MS365DetailsCompositeHelper helper;

    private String connectionName;
    private Shell shell;
    private Composite wConnectionTypeComp;
    private Text wName;
    private CCombo wConnectionType;
    private Text wDescription;
    private Composite wDetailsWrapperComp;
    private ScrolledComposite wScrolledComposite;

    private ConnectionDetailsInterface selectedDetails;
    private GraphConnectionType.GraphConnectionTypeComposite graphConnectionTypeComposite;

    public ConnectionDialog(Shell shell, int width, int height){
        super(shell, SWT.NONE);
        this.width = width;
        this.height = height;
        this.props = PropsUI.getInstance();
        this.connectionTypes = this.connectionManager.provideTypes();
        this.helper = new MS365DetailsCompositeHelper(PKG, props);
    }

    public void open(String title){
        this.open(title, null);
    }

    public void open(String title, String existingConnectionName) {
        this.connectionName = existingConnectionName;
        Shell parent = getParent();
        shell = new Shell( parent, SWT.DIALOG_TRIM | SWT.RESIZE | SWT.MAX | SWT.MIN );
        shell.addShellListener(shellListener);
        shell.setSize( width, height );
        props.setLook( shell );
        shell.setImage( LOGO );

        FormLayout formLayout = new FormLayout();
        formLayout.marginWidth = Const.FORM_MARGIN;
        formLayout.marginHeight = Const.FORM_MARGIN;

        shell.setText( title );
        shell.setLayout( formLayout );

        // First, add the buttons...
        Button wOK = new Button( shell, SWT.PUSH );
        wOK.setText( BaseMessages.getString( PKG, "System.Button.OK" ) );

        Button wCancel = new Button( shell, SWT.PUSH );
        wCancel.setText( BaseMessages.getString( PKG, "System.Button.Cancel" ) );

        Button wTest = new Button( shell, SWT.PUSH );
        wTest.setText( BaseMessages.getString( PKG, "System.Button.Test" ) );

        Button[] buttons = new Button[] { wOK, wCancel, wTest };
        BaseStepDialog.positionBottomRightButtons( shell, buttons, MARGIN, null );

        String docUrl =
                Const.getDocUrl( BaseMessages.getString( PKG, "ConnectionDialog.help.dialog.Help" ) );
        String docTitle = BaseMessages.getString( PKG, "ConnectionDialog.help.dialog.Title" );
        String docHeader = BaseMessages.getString( PKG, "ConnectionDialog.help.dialog.Header" );
        Button btnHelp = new Button( shell, SWT.NONE );
        btnHelp.setImage( GUIResource.getInstance().getImageHelpWeb() );
        btnHelp.setText( BaseMessages.getString( PKG, "System.Button.Help" ) );
        btnHelp.setToolTipText( BaseMessages.getString( PKG, "System.Tooltip.Help" ) );
        BaseStepDialog.positionBottomRightButtons( shell, new Button[] { btnHelp }, MARGIN, null );
        btnHelp.addSelectionListener( new SelectionAdapter() {
            @Override
            public void widgetSelected( SelectionEvent evt ) {
                HelpUtils.openHelpDialog( parent.getShell(), docTitle,
                        docUrl, docHeader );
            }
        } );

        // Add listeners
        wOK.addListener( SWT.Selection, e -> ok() );
        wCancel.addListener( SWT.Selection, e -> cancel() );
        wTest.addListener( SWT.Selection, e -> test() );

        // The rest stays above the buttons...
        wConnectionTypeComp = new Composite( shell, SWT.BORDER );
        props.setLook( wConnectionTypeComp );

        FormLayout genLayout = new FormLayout();
        genLayout.marginWidth = Const.FORM_MARGIN;
        genLayout.marginHeight = Const.FORM_MARGIN;
        wConnectionTypeComp.setLayout( genLayout );

        FormData fdTabFolder = new FormData();
        fdTabFolder.top = new FormAttachment( 0, MARGIN );
        fdTabFolder.left = new FormAttachment( 0, 0 );
        fdTabFolder.right = new FormAttachment( 100, 0 );
        fdTabFolder.bottom = new FormAttachment( wCancel, -MARGIN );
        wConnectionTypeComp.setLayoutData( fdTabFolder );

        addHeaderWidgets();

        // Detect X or ALT-F4 or something that kills this window...
        shell.addShellListener( new ShellAdapter() {
            @Override
            public void shellClosed( ShellEvent e ) {
                cancel();
            }
        } );

        shell.open();
        while ( !shell.isDisposed() ) {
            Display display = parent.getDisplay();
            if ( !display.readAndDispatch() ) {
                display.sleep();
            }
        }
    }

    private void addHeaderWidgets(){
        // Connection Name
        Label wlName = createLabel( "ConnectionDialog.ConnectionName.Label", null );
        wName = createText( wlName );

        // Connection Type
        Label wlConnectionType = createLabel( "ConnectionDialog.ConnectionType.Label", wName );
        wConnectionType = createCCombo( wlConnectionType, 200 );
        wConnectionType.setItems(connectionTypes.stream().map(ConnectionTypeInterface::provideType).toArray(String[]::new));
        wConnectionType.select( 0 );

        //Description
        Label wlDescription = createLabel( "ConnectionDialog.Description.Label", wConnectionType );
        wDescription = createMultilineText( wlDescription );
        ( (FormData) wDescription.getLayoutData() ).height = wDescription.computeSize( SWT.DEFAULT, SWT.DEFAULT ).y * 3;

        setConnectionType();
        populateWidgets();

        wName.addModifyListener(modifyEvent -> {
                ConnectionDialog.this.selectedDetails.setConnectionName(wName.getText());
                this.selectedDetails.setHasDuplicateConnectionName(this.connectionManager.hasConnectionName(this.selectedDetails.getConnectionName()));
            }
        );
        wDescription.addModifyListener(modifyEvent -> ConnectionDialog.this.selectedDetails.setDescription(wDescription.getText()));
        wConnectionType.addSelectionListener( new SelectionAdapter() {
            @Override public void widgetSelected( SelectionEvent selectionEvent ) {
                String connectionTypeSelected = wConnectionType.getText();
                //Expected behaviour if switching back to same type is to show error because of existing connection name
                if (!ConnectionDialog.this.selectedDetails.provideType().provideType().equals(connectionTypeSelected)) {
                    ConnectionTypeInterface connectionTypeInterface = connectionTypes.stream()
                            .filter(type -> type.provideType().equals(connectionTypeSelected))
                            .findFirst().orElse(connectionTypes.get(0));
                    ConnectionDialog.this.selectedDetails = ConnectionDialog.this.selectedDetails.copyToNewDetailsInterface(connectionTypeInterface);
                    updateConnectionType();
                }
            }
        });
    }


    private void updateConnectionType(){
        if (this.graphConnectionTypeComposite != null && this.selectedDetails != null){
            this.selectedDetails.provideType().closeDialog();
            this.graphConnectionTypeComposite = null;
        } else {
           if (wDetailsWrapperComp != null && !wDetailsWrapperComp.isDisposed()){
               wDetailsWrapperComp.dispose();
               wDetailsWrapperComp = null;
           }
        }
        if (wScrolledComposite != null && !wScrolledComposite.isDisposed()){
            wScrolledComposite.dispose();
        }

        wScrolledComposite = new ScrolledComposite( wConnectionTypeComp, SWT.BORDER + SWT.V_SCROLL );
        FormData fdScrolledComp = new FormData();
        fdScrolledComp.top = new FormAttachment( wDescription, MARGIN * 2 );
        fdScrolledComp.left = new FormAttachment( 0, 0 );
        fdScrolledComp.right = new FormAttachment( 100, 0);
        fdScrolledComp.bottom = new FormAttachment( 100, 0 );
        wScrolledComposite.setLayoutData( fdScrolledComp );

        wDetailsWrapperComp = new Composite( wScrolledComposite, SWT.NONE );
        props.setLook( wDetailsWrapperComp );
        wDetailsWrapperComp.setLayout(new FormLayout());

        if ( this.selectedDetails != null ) {
            log.info("Create components of Graph dialog");
            UITypeCallback uiTypeCallback = this.selectedDetails.provideCallback();
            this.graphConnectionTypeComposite = this.selectedDetails.provideType().openDialog(shell.getDisplay(), this.helper, uiTypeCallback, wDetailsWrapperComp, width);
        }

        wScrolledComposite.setExpandHorizontal( true );
        wScrolledComposite.setExpandVertical( true );
        wScrolledComposite.setContent( wDetailsWrapperComp );
        wDetailsWrapperComp.pack();
        wScrolledComposite.setMinSize(
                wDetailsWrapperComp.computeSize( wConnectionTypeComp.getClientArea().width, SWT.DEFAULT ) );
        wConnectionTypeComp.layout();

        //restore saved values for existing connection details
        if (this.selectedDetails.getConnectionName() != null && this.selectedDetails.getConnectionName().length() > 0){
            this.wName.setText(this.selectedDetails.getConnectionName());
            this.wDescription.setText(this.selectedDetails.getDescription());
            this.selectedDetails.provideType().insertUIValues(this.selectedDetails);
        }
    }

    private void setConnectionType() {
        connectionManager.createCopyOfDetailsForComparison();
        if ( connectionName != null ) {
            this.selectedDetails = connectionManager.provideDetailsByConnectionName(connectionName);
            return;
        }
        this.selectedDetails = connectionTypes.get(0).createInstanceOfDetails();
    }

    private void populateWidgets(){
        wConnectionType.select(this.connectionTypes.indexOf(this.selectedDetails.provideType()));
        this.updateConnectionType();
        wName.setText(Const.NVL(this.selectedDetails.getConnectionName(), ""));
        wDescription.setText(Const.NVL(this.selectedDetails.getDescription(), ""));
    }

    private void cancel() {
        dispose();
    }

    public void dispose() {
        props.setScreen( new WindowProperty( shell ) );
        shell.dispose();
        this.selectedDetails.setHasDuplicateConnectionName(false);
    }

    private void test(){
        boolean testResult = this.selectedDetails.test();
        if (testResult){
            MessageBox mb = new MessageBox(shell, SWT.OK | SWT.ICON_INFORMATION);
            mb.setMessage(BaseMessages.getString(PKG, "ConnectionDialog.Message.Test"));
            mb.open();
        } else {
            MessageBox mb = new MessageBox(shell, SWT.OK | SWT.ICON_ERROR);
            mb.setMessage(BaseMessages.getString(PKG, "ConnectionDialog.Message.TestFailed"));
            mb.open();
        }
    }

    private void ok(){
        if ( this.validateEntries() ) {
            dispose();
        }
    }

    private boolean validateEntries(){
        String validationMessage = this.selectedDetails.computeValidateMessage();
        if ( !isEmpty( validationMessage)) {
            MessageBox mb = new MessageBox( shell, SWT.OK | SWT.ICON_ERROR );
            mb.setMessage( validationMessage );
            mb.open();
            return false;
        }
        if (this.selectedDetails.hasDuplicateConnectionName()){
            MessageBox mb = new MessageBox(shell, SWT.OK | SWT.ICON_ERROR);
            mb.setMessage(BaseMessages.getString(PKG, "ConnectionDialog.Message.NameExists"));
            mb.open();
            return false;
        }
        //run connection test, add to DB and add to Menu
        new Thread(() -> {
            boolean successfulTest = this.selectedDetails.test();
            if (!successfulTest){
                Shell shell = Spoon.getInstance().getShell();
                Display display = shell.getDisplay();
                display.asyncExec(() -> {
                    MessageBox mb = new MessageBox( shell, SWT.OK | SWT.ICON_ERROR );
                    mb.setMessage(BaseMessages.getString(PKG, "ConnectionDialog.Message.TestFailed"));
                    mb.open();
                });
            } else {
                this.selectedDetails.onInitiatedByUser();
                connectionManager.saveConnectionDetailsInterface(this.selectedDetails);
                ConnectionDelegate.refreshMenu();
            }
        }).start();
        return true;
    }

    private Label createLabel( String key, Control topWidget ) {
        return helper.createLabel( wConnectionTypeComp, SWT.LEFT | SWT.WRAP, key, topWidget );
    }

    private Text createText( Control topWidget ) {
        return helper.createText( wConnectionTypeComp, TEXT_VAR_FLAGS, topWidget, 0 );
    }

    private CCombo createCCombo( Control topWidget, int width ) {
        return helper.createCCombo( wConnectionTypeComp, TEXT_VAR_FLAGS, topWidget, width );
    }

    private Text createMultilineText( Control topWidget ) {
        return helper.createText( wConnectionTypeComp, SWT.MULTI | SWT.LEFT | SWT.BORDER | SWT.V_SCROLL, topWidget, 0 );
    }

    private ShellListener shellListener = new ShellListener() {
        @Override
        public void shellActivated(ShellEvent shellEvent) {

        }

        @Override
        public void shellClosed(ShellEvent shellEvent) {
            ConnectionDialog.this.selectedDetails.setHasDuplicateConnectionName(false);
        }

        @Override
        public void shellDeactivated(ShellEvent shellEvent) {

        }

        @Override
        public void shellDeiconified(ShellEvent shellEvent) {

        }

        @Override
        public void shellIconified(ShellEvent shellEvent) {

        }
    };
}
