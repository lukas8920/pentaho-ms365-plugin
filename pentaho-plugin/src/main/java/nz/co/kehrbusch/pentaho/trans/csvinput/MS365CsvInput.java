package nz.co.kehrbusch.pentaho.trans.csvinput;

import org.pentaho.di.trans.Trans;
import org.pentaho.di.trans.TransMeta;
import org.pentaho.di.trans.step.StepDataInterface;
import org.pentaho.di.trans.step.StepMeta;
import org.pentaho.di.trans.step.StepMetaInterface;
import org.pentaho.di.trans.steps.csvinput.CsvInput;

public class MS365CsvInput extends CsvInput {
    public MS365CsvInput(StepMeta stepMeta, StepDataInterface stepDataInterface, int copyNr, TransMeta transMeta, Trans trans) {
        super(stepMeta, stepDataInterface, copyNr, transMeta, trans);
    }

    @Override
    public void dispose(StepMetaInterface smi, StepDataInterface sdi ) {

        // Casting to step-specific implementation classes is safe
        MS365CsvInputMeta meta = (MS365CsvInputMeta) smi;
        MS365CsvInputData data = (MS365CsvInputData) sdi;

        // Add any step-specific initialization that may be needed here

        // Call superclass dispose()
        super.dispose( meta, data );
    }
}
