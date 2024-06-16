package nz.co.kehrbusch.pentaho.trans.csvinput;

import nz.co.kehrbusch.pentaho.connections.manage.GraphConnectionDetails;
import nz.co.kehrbusch.pentaho.connections.manage.MS365ConnectionManager;
import nz.co.kehrbusch.pentaho.util.ms365opensavedialog.MS365OpenSaveDialog;
import nz.co.kehrbusch.pentaho.util.ms365opensavedialog.providers.MS365FileProvider;
import nz.co.kehrbusch.pentaho.util.ms365opensavedialog.providers.MS36File;
import org.eclipse.swt.custom.CCombo;
import org.eclipse.swt.events.*;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.widgets.*;
import org.eclipse.swt.widgets.Button;
import org.pentaho.di.core.Const;
import org.pentaho.di.core.logging.LogChannel;
import org.pentaho.di.i18n.BaseMessages;
import org.pentaho.di.plugins.fileopensave.service.ProviderServiceService;
import org.pentaho.di.trans.TransMeta;
import org.pentaho.di.ui.core.FileDialogOperation;
import org.pentaho.di.ui.core.widget.TextVar;
import org.pentaho.di.ui.trans.steps.csvinput.CsvInputDialog;

import javax.swing.*;
import javax.swing.filechooser.FileFilter;
import java.awt.*;
import java.io.File;
import java.util.Arrays;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class MS365CsvInputDialog extends CsvInputDialog {
    private static Class<?> PKG = MS365CsvInputDialog.class;
    private static final int WIDTH = ( Const.isOSX() || Const.isLinux() ) ? 930 : 947;
    private static final int HEIGHT = ( Const.isOSX() || Const.isLinux() ) ? 618 : 626;

    private static final Logger log = Logger.getLogger(MS365CsvInputDialog.class.getName());

    private TextVar wFilename = null;
    private MS36File selectedFile;
    private boolean isInitialized = false;

    public MS365CsvInputDialog(Shell parent, Object in, TransMeta tr, String sname) {
        super(parent, in, tr, sname);
    }

    public String open(){
        ((MS365CsvInputMeta) MS365CsvInputDialog.this.baseStepMeta).setMs365CsvInputDialog(this);
        return super.open();
    }

    private void addControls(){
        Control[] controls = this.shell.getChildren();
        int margin = 4;
        log.info("Initiate controls");
        boolean isReceivingInput = this.transMeta.findNrPrevSteps(this.stepMeta) > 0;

        CCombo cCombo = null;
        if(isReceivingInput){
            log.info("Is Receiving Input");
            cCombo = (CCombo) Arrays.stream(controls).filter(control -> control instanceof CCombo).collect(Collectors.toList()).get(0);
        } else {
            wFilename = (TextVar) Arrays.stream(controls).filter(control -> control instanceof TextVar).collect(Collectors.toList()).get(0);
        }

        Button wbbFilename = (Button) Arrays.stream(controls).filter(control -> control instanceof Button && ((Button) control).getText().equals(BaseMessages.getString(PKG, "System.Button.Browse", new String[0])))
                .findFirst().orElse(null);

        wbbFilename.dispose();
        wbbFilename = new Button(this.shell, 16777224);
        this.props.setLook(wbbFilename);
        wbbFilename.setText(BaseMessages.getString(PKG, "System.Button.Browse", new String[0]));
        wbbFilename.setToolTipText(BaseMessages.getString(PKG, "System.Tooltip.BrowseForFileOrDirAndAdd", new String[0]));
        FormData fdbFilename = new FormData();
        fdbFilename.top = new FormAttachment(wStepname, margin);
        fdbFilename.right = new FormAttachment(100, 0);
        wbbFilename.setLayoutData(fdbFilename);
        wbbFilename.addSelectionListener(new SelectionListener() {
            @Override
            public void widgetSelected(SelectionEvent selectionEvent) {
                MS365ConnectionManager connectionManager = MS365ConnectionManager.getInstance();
                //todo replace in CSV Input
                GraphConnectionDetails graphConnectionDetails = (GraphConnectionDetails) connectionManager.getConnections().get(0);

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
            }

            @Override
            public void widgetDefaultSelected(SelectionEvent selectionEvent) {

            }
        });

        if (isReceivingInput){
            FormData formData = (FormData) cCombo.getLayoutData();
            formData.top = new FormAttachment(wStepname, margin);
            cCombo.setLayoutData(formData);
        } else {
            FormData formData = (FormData) wFilename.getLayoutData();
            formData.top = new FormAttachment(wStepname, margin);
            formData.right = new FormAttachment(wbbFilename, -margin);
            wFilename.setLayoutData(formData);
        }
    }

    public void attachAdditionalFields(){
        log.info("Call read controls.");
        if (!this.isInitialized){
            addControls();
            this.isInitialized = true;
        }
    }

    private File openFileChooser() {
        JFrame mainWindow = new JFrame();
        mainWindow.setSize( 750, 700 );
        mainWindow.setTitle( "Hitachi Vantara - Update Jobs and Transformations for import into Data Flow Manager" );
        mainWindow.setLocationRelativeTo( null );
        mainWindow.setDefaultCloseOperation( JFrame.EXIT_ON_CLOSE );
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setAcceptAllFileFilterUsed( false );
        fileChooser.setPreferredSize( new Dimension( 750, 400 ) );
        fileChooser.setCurrentDirectory( new File( "." ) );
        fileChooser.setFileSelectionMode( JFileChooser.FILES_AND_DIRECTORIES );
        fileChooser.addChoosableFileFilter( new FileFilter() {
            public String getDescription() {
                return "Kettle Transformations (*.ktr) and Kettle Jobs (*.kjb)";
            }

            public boolean accept( File file ) {
                if ( file.isDirectory() ) {
                    return true;
                } else {
                    return file.getName().toLowerCase().endsWith( ".ktr" ) || file.getName().toLowerCase().endsWith( ".kjb" );
                }
            }
        } );
        return fileChooser.showOpenDialog( mainWindow ) == JFileChooser.APPROVE_OPTION ? fileChooser.getSelectedFile()
                : null;
    }
}
