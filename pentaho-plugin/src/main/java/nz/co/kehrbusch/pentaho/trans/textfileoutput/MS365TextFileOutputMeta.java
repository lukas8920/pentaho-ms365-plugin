package nz.co.kehrbusch.pentaho.trans.textfileoutput;

import org.eclipse.swt.widgets.Shell;
import org.pentaho.di.core.annotations.Step;
import org.pentaho.di.core.database.DatabaseMeta;
import org.pentaho.di.core.exception.KettleException;
import org.pentaho.di.core.exception.KettleXMLException;
import org.pentaho.di.core.row.ValueMetaInterface;
import org.pentaho.di.core.util.Utils;
import org.pentaho.di.core.xml.XMLHandler;
import org.pentaho.di.repository.ObjectId;
import org.pentaho.di.repository.Repository;
import org.pentaho.di.trans.Trans;
import org.pentaho.di.trans.TransMeta;
import org.pentaho.di.trans.step.*;
import org.pentaho.di.trans.steps.textfileoutput.TextFileField;
import org.pentaho.di.trans.steps.textfileoutput.TextFileOutputMeta;
import org.pentaho.metastore.api.IMetaStore;
import org.w3c.dom.Node;

import java.util.List;

@Step(
        id = "MS365TextFileOutput",
        name = "MS365TextFileOutput.Name",
        description = "MS365TextFileOutput.TooltipDesc",
        image = "images/TFO.svg",
        categoryDescription = "MS365Plugin.Category.MS365Output",
        i18nPackageName = "nz.co.kehrbusch.pentaho.trans.textfileoutput",
        documentationUrl = "MS365TextFileOutput.DocumentationURL"
)
public class MS365TextFileOutputMeta extends TextFileOutputMeta {
    private MS365TextFileOutputDialog ms365TextFileOutputDialog = null;
    private String connectionName;

    public StepDialogInterface getDialog(Shell shell, StepMetaInterface meta, TransMeta transMeta, String name ) {
        return new MS365TextFileOutputDialog( shell, meta, transMeta, name );
    }

    @Override
    public StepInterface getStep(StepMeta stepMeta, StepDataInterface stepDataInterface, int cnr, TransMeta transMeta,
                                 Trans disp ) {
        return new MS365TextFileOutput(stepMeta, stepDataInterface, cnr, transMeta, disp);
    }

    @Override
    public StepDataInterface getStepData() {
        return new MS365TextFileOutputData();
    }

    @Override
    public void setChanged(boolean changed){
        super.setChanged(changed);
        if (this.ms365TextFileOutputDialog != null){
            ms365TextFileOutputDialog.attachAdditionalFields();
        }
    }

    @Override
    public void loadXML(Node stepnode, List<DatabaseMeta> databases, IMetaStore metaStore) throws KettleXMLException {
        super.loadXML(stepnode, databases, metaStore);
        this.readXML(stepnode);
    }

    private void readXML(Node stepnode) {
        this.connectionName = XMLHandler.getTagValue(stepnode, "connection_name");
    }

    @Override
    public String getXML() {
        String parentXml = super.getXML();
        StringBuilder builder = new StringBuilder(parentXml);
        builder.insert(0, "    " + XMLHandler.addTagValue("connection_name", this.connectionName));
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

    public void setMS365TextFileOutputDialog(MS365TextFileOutputDialog ms365TextFileOutputDialog) {
        this.ms365TextFileOutputDialog = ms365TextFileOutputDialog;
    }

    public String getConnectionName() {
        return connectionName;
    }

    public void setConnectionName(String connectionName) {
        this.connectionName = connectionName;
    }

    protected synchronized void calcMetaWithFieldOptions(MS365TextFileOutputData data) {
        if (null == this.metaWithFieldOptions) {
            if (!Utils.isEmpty(this.getOutputFields())) {
                this.metaWithFieldOptions = new ValueMetaInterface[this.getOutputFields().length];

                for(int i = 0; i < this.getOutputFields().length; ++i) {
                    ValueMetaInterface v = data.outputRowMeta.getValueMeta(data.fieldnrs[i]);
                    if (v != null) {
                        this.metaWithFieldOptions[i] = v.clone();
                        TextFileField field = this.getOutputFields()[i];
                        this.metaWithFieldOptions[i].setLength(field.getLength());
                        this.metaWithFieldOptions[i].setPrecision(field.getPrecision());
                        if (!Utils.isEmpty(field.getFormat())) {
                            this.metaWithFieldOptions[i].setConversionMask(field.getFormat());
                        }

                        this.metaWithFieldOptions[i].setDecimalSymbol(field.getDecimalSymbol());
                        this.metaWithFieldOptions[i].setGroupingSymbol(field.getGroupingSymbol());
                        this.metaWithFieldOptions[i].setCurrencySymbol(field.getCurrencySymbol());
                        this.metaWithFieldOptions[i].setTrimType(field.getTrimType());
                        if (!Utils.isEmpty(this.getEncoding())) {
                            this.metaWithFieldOptions[i].setStringEncoding(this.getEncoding());
                        }

                        this.metaWithFieldOptions[i].setOutputPaddingEnabled(true);
                    }
                }
            } else {
                this.metaWithFieldOptions = null;
            }
        }
    }

    protected synchronized ValueMetaInterface[] getMetaWithFieldOptions() {
        return super.getMetaWithFieldOptions();
    }
}
