package nz.co.kehrbusch.pentaho.trans.csvinput;

import org.eclipse.swt.widgets.Shell;
import org.pentaho.di.core.KettleAttributeInterface;
import org.pentaho.di.core.annotations.Step;
import org.pentaho.di.core.injection.InjectionSupported;
import org.pentaho.di.trans.Trans;
import org.pentaho.di.trans.TransMeta;
import org.pentaho.di.trans.step.*;
import org.pentaho.di.trans.steps.csvinput.CsvInputMeta;

import java.util.logging.Logger;

@Step(
        id = "MS365CsvInput",
        name = "MS365CsvInput.Name",
        description = "MS365CsvInput.TooltipDesc",
        image = "images/environments.svg",
        categoryDescription = "i18n:org.pentaho.di.trans.step:BaseStep.Category.Input",
        i18nPackageName = "nz.co.kehrbusch.pentaho.trans.csvinput",
        documentationUrl = "MS365CsvInput.DocumentationURL"
)
@InjectionSupported( localizationPrefix = "MS365CsvInput.Injection." )
public class MS365CsvInputMeta extends CsvInputMeta implements StepMetaInjectionInterface {
    private static final Logger log = Logger.getLogger(MS365CsvInputMeta.class.getName());

    private MS365CsvInputDialog ms365CsvInputDialog;

    public MS365CsvInputMeta(){
        super();
    }

    public StepDialogInterface getDialog(Shell shell, StepMetaInterface meta, TransMeta transMeta, String name ) {
        return new MS365CsvInputDialog( shell, meta, transMeta, name );
    }

    @Override
    public StepInterface getStep(StepMeta stepMeta, StepDataInterface stepDataInterface, int cnr, TransMeta transMeta,
                                 Trans disp ) {
        return new MS365CsvInput( stepMeta, stepDataInterface, cnr, transMeta, disp );
    }

    @Override
    public StepDataInterface getStepData() {
        return new MS365CsvInputData();
    }

    //A bit hacky, but entrypoint for dialog injection
    @Override
    public void setChanged(boolean changed){
        super.setChanged(changed);
        log.info("check ms365csvinputdialog");
        if (this.ms365CsvInputDialog != null){
            ms365CsvInputDialog.attachAdditionalFields();
        }
    }

    public void setMs365CsvInputDialog(MS365CsvInputDialog ms365CsvInputDialog){
        this.ms365CsvInputDialog = ms365CsvInputDialog;
    }
}
