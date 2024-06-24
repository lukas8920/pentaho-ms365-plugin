package nz.co.kehrbusch.pentaho.trans.textfileinput;

import nz.co.kehrbusch.pentaho.util.file.SharepointWrapperMeta;
import org.eclipse.swt.widgets.Shell;
import org.pentaho.di.core.annotations.Step;
import org.pentaho.di.core.database.DatabaseMeta;
import org.pentaho.di.core.exception.KettleException;
import org.pentaho.di.core.exception.KettleXMLException;
import org.pentaho.di.core.xml.XMLHandler;
import org.pentaho.di.repository.ObjectId;
import org.pentaho.di.repository.Repository;
import org.pentaho.di.trans.Trans;
import org.pentaho.di.trans.TransMeta;
import org.pentaho.di.trans.step.*;
import org.pentaho.di.trans.steps.fileinput.text.TextFileInputMeta;
import org.pentaho.metastore.api.IMetaStore;
import org.w3c.dom.Node;

import java.util.List;

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

    public void setConnectionName(String connectionName){
        this.connectionName = connectionName;
    }

    @Override
    public void loadXML(Node stepnode, List<DatabaseMeta> databases, IMetaStore metaStore) throws KettleXMLException {
        super.loadXML(stepnode, databases, metaStore);
        this.readData(stepnode);
    }

    private void readData(Node stepnode){
        this.connectionName = XMLHandler.getTagValue(stepnode, "connection_name");
    }

    @Override
    public String getXML(){
        String parentXml = super.getXML();
        StringBuilder builder = new StringBuilder(parentXml);
        builder.insert(0, "    "  + XMLHandler.addTagValue("connection_name", this.connectionName));
        return builder.toString();
    }

    @Override
    public void readRep(Repository rep, IMetaStore metaStore, ObjectId id_step, List<DatabaseMeta> databases) throws KettleException {
        super.readRep(rep, metaStore, id_step, databases);
        this.connectionName = rep.getStepAttributeString(id_step, "connection_name");
    }

    @Override
    public void saveRep(Repository rep, IMetaStore metaStore, ObjectId id_transformation, ObjectId id_step) throws KettleException {
        super.saveRep(rep, metaStore, id_transformation, id_step);
        rep.saveStepAttribute(id_transformation, id_step, "connection_name", this.connectionName);
    }
}