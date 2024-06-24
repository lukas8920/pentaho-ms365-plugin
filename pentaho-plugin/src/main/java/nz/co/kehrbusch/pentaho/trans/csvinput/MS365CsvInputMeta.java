package nz.co.kehrbusch.pentaho.trans.csvinput;

import nz.co.kehrbusch.pentaho.util.file.SharepointWrapperMeta;
import org.eclipse.swt.widgets.Shell;
import org.pentaho.di.core.annotations.Step;
import org.pentaho.di.core.database.DatabaseMeta;
import org.pentaho.di.core.exception.KettleException;
import org.pentaho.di.core.exception.KettleXMLException;
import org.pentaho.di.core.injection.InjectionSupported;
import org.pentaho.di.core.xml.XMLHandler;
import org.pentaho.di.repository.ObjectId;
import org.pentaho.di.repository.Repository;
import org.pentaho.di.trans.Trans;
import org.pentaho.di.trans.TransMeta;
import org.pentaho.di.trans.step.*;
import org.pentaho.di.trans.steps.csvinput.CsvInputMeta;
import org.pentaho.metastore.api.IMetaStore;
import org.w3c.dom.Node;

import java.util.List;
import java.util.logging.Logger;

@Step(
        id = "MS365CsvInput",
        name = "MS365CsvInput.Name",
        description = "MS365CsvInput.TooltipDesc",
        image = "images/CSV.svg",
        categoryDescription = "MS365Plugin.Category.MS365Input",
        i18nPackageName = "nz.co.kehrbusch.pentaho.trans.csvinput",
        documentationUrl = "MS365CsvInput.DocumentationURL"
)
@InjectionSupported( localizationPrefix = "MS365CsvInput.Injection." )
public class MS365CsvInputMeta extends CsvInputMeta implements StepMetaInjectionInterface {
    private static final Logger log = Logger.getLogger(MS365CsvInputMeta.class.getName());

    private MS365CsvInputDialog ms365CsvInputDialog;
    private String connectionName;

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
        if (this.ms365CsvInputDialog != null){
            ms365CsvInputDialog.attachAdditionalFields();
        }
    }

    public void setMs365CsvInputDialog(MS365CsvInputDialog ms365CsvInputDialog){
        this.ms365CsvInputDialog = ms365CsvInputDialog;
    }

    @Override
    public void loadXML(Node stepnode, List<DatabaseMeta> databases, IMetaStore metaStore) throws KettleXMLException {
        super.loadXML(stepnode, databases, metaStore);
        this.readData(stepnode);
    }

    private void readData(Node stepnode){
        this.connectionName = XMLHandler.getTagValue(stepnode, this.getXmlCode("CONNECTION_NAME"));
    }

    @Override
    public String getXML(){
        String parentXml = super.getXML();
        StringBuilder builder = new StringBuilder(parentXml);
        builder.insert(0, "    "  + XMLHandler.addTagValue(this.getXmlCode("CONNECTION_NAME"), this.connectionName));
        return builder.toString();
    }

    @Override
    public void readRep(Repository rep, IMetaStore metaStore, ObjectId id_step, List<DatabaseMeta> databases) throws KettleException {
        super.readRep(rep, metaStore, id_step, databases);
        this.connectionName = rep.getStepAttributeString(id_step, this.getRepCode("CONNECTION_NAME"));
    }

    @Override
    public void saveRep(Repository rep, IMetaStore metaStore, ObjectId id_transformation, ObjectId id_step) throws KettleException {
        super.saveRep(rep, metaStore, id_transformation, id_step);
        rep.saveStepAttribute(id_transformation, id_step, this.getRepCode("CONNECTION_NAME"), this.connectionName);
    }

    public String getConnectionName(){
        return this.connectionName;
    }

    public void setConnectionName(String connectionName){
        this.connectionName = connectionName;
    }
}
