package nz.co.kehrbusch.pentaho.trans.textfileinput;

import org.eclipse.swt.widgets.Shell;
import org.pentaho.di.core.annotations.Step;
import org.pentaho.di.trans.Trans;
import org.pentaho.di.trans.TransMeta;
import org.pentaho.di.trans.step.*;
import org.pentaho.di.trans.steps.fileinput.text.TextFileInputMeta;

@Step(
        id = "MS365TextFileInput",
        name = "MS365TextFileInput.Name",
        image = "images/TFI.svg",
        categoryDescription = "MS365Plugin.Category.MS365Input",
        i18nPackageName = "nz.co.kehrbusch.pentaho.trans.textfileinput",
        documentationUrl = "MS365TextFileInput.Documentation"
)
public class MS365TextFileInputMeta extends TextFileInputMeta {
    private MS365TextFileInputDialog ms365TextFileInputDialog;

    private String connectionName;

    public MS365TextFileInputMeta() {
        super();
    }

    public StepDialogInterface getDialog(Shell shell, StepMetaInterface meta, TransMeta transMeta, String name) {
        return new MS365TextFileInputDialog(shell, meta, transMeta, name);
    }

    @Override
    public StepInterface getStep(StepMeta stepMeta, StepDataInterface stepDataInterface, int cnr, TransMeta transMeta,
                                 Trans disp) {
        return new MS365TextFileInput(stepMeta, stepDataInterface, cnr, transMeta, disp);
    }

    @Override
    public StepDataInterface getStepData() {
        return new MS365TextFileInputData();
    }

    //Injection point for dialog access
    @Override
    public String getErrorTextField(){
        if (this.ms365TextFileInputDialog != null){
            ms365TextFileInputDialog.attachAdditionalFields();
        }
        return super.getErrorTextField();
    }

    public void setMs365TextFileInputDialog(MS365TextFileInputDialog ms365TextFileInputDialog){
        this.ms365TextFileInputDialog = ms365TextFileInputDialog;
    }

    public String getConnectionName() {
        return connectionName;
    }
}