package nz.co.kehrbusch.pentaho.trans.getfilenames;

import nz.co.kehrbusch.ms365.interfaces.ISharepointConnection;
import nz.co.kehrbusch.pentaho.util.file.SharepointFileWrapper;
import org.eclipse.swt.widgets.Shell;
import org.pentaho.di.core.annotations.Step;
import org.pentaho.di.core.database.DatabaseMeta;
import org.pentaho.di.core.exception.KettleException;
import org.pentaho.di.core.exception.KettleStepException;
import org.pentaho.di.core.exception.KettleXMLException;
import org.pentaho.di.core.logging.LogChannelInterface;
import org.pentaho.di.core.row.RowMetaInterface;
import org.pentaho.di.core.row.ValueMetaInterface;
import org.pentaho.di.core.row.value.ValueMetaInteger;
import org.pentaho.di.core.row.value.ValueMetaString;
import org.pentaho.di.core.variables.VariableSpace;
import org.pentaho.di.core.xml.XMLHandler;
import org.pentaho.di.repository.ObjectId;
import org.pentaho.di.repository.Repository;
import org.pentaho.di.trans.Trans;
import org.pentaho.di.trans.TransMeta;
import org.pentaho.di.trans.step.*;
import org.pentaho.di.trans.steps.getfilenames.GetFileNamesMeta;
import org.pentaho.metastore.api.IMetaStore;
import org.w3c.dom.Node;

import java.util.List;

@Step(
        id="MS365GetFileNames",
        name = "MS365GetFileNames.Name",
        image = "images/GFN.svg",
        categoryDescription = "MS365Plugin.Category.MS365Input",
        i18nPackageName = "nz.co.kehrbusch.pentaho.trans.getfilenames",
        documentationUrl = "MS365GetFileNames.Documentation"
)
public class MS365GetFileNamesMeta extends GetFileNamesMeta {
    private MS365GetFileNamesDialog ms365GetFileNamesDialog;
    private String connectionName;

    public StepDialogInterface getDialog(Shell shell, StepMetaInterface meta, TransMeta transMeta, String name) {
        return new MS365GetFileNamesDialog(shell, meta, transMeta, name);
    }

    @Override
    public StepInterface getStep(StepMeta stepMeta, StepDataInterface stepDataInterface, int cnr, TransMeta transMeta,
                                 Trans disp) {
        return new MS365GetFileNames(stepMeta, stepDataInterface, cnr, transMeta, disp);
    }

    @Override
    public StepDataInterface getStepData() {
        return new MS365GetFileNamesData();
    }

    @Override
    public void setChanged(boolean changed){
        super.setChanged(changed);
        if (this.ms365GetFileNamesDialog != null){
            ms365GetFileNamesDialog.attachAdditionalFields();
        }
    }

    public void setMs365GetFileNamesDialog(MS365GetFileNamesDialog ms365GetFileNamesDialog){
        this.ms365GetFileNamesDialog = ms365GetFileNamesDialog;
    }

    public String getConnectionName() {
        return connectionName;
    }

    public void setConnectionName(String connectionName) {
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

    @Override
    public void getFields(RowMetaInterface row, String name, RowMetaInterface[] info, StepMeta nextStep, VariableSpace space, Repository repository, IMetaStore metaStore) throws KettleStepException {
        ValueMetaInterface filename = new ValueMetaString("filename");
        filename.setLength(500);
        filename.setPrecision(-1);
        filename.setOrigin(name);
        row.addValueMeta(filename);
        ValueMetaInterface short_filename = new ValueMetaString("path");
        short_filename.setLength(500);
        short_filename.setPrecision(-1);
        short_filename.setOrigin(name);
        row.addValueMeta(short_filename);
        ValueMetaInterface type = new ValueMetaString("type");
        type.setLength(500);
        type.setPrecision(-1);
        type.setOrigin(name);
        row.addValueMeta(type);
        ValueMetaInterface createdDate = new ValueMetaString("created_date");
        type.setLength(500);
        type.setPrecision(-1);
        type.setOrigin(name);
        row.addValueMeta(createdDate);
        ValueMetaInterface modifiedDate = new ValueMetaString("modified_date");
        type.setLength(500);
        type.setPrecision(-1);
        type.setOrigin(name);
        row.addValueMeta(modifiedDate);
        ValueMetaInterface webUrl = new ValueMetaString("web_url");
        type.setLength(500);
        type.setPrecision(-1);
        type.setOrigin(name);
        row.addValueMeta(webUrl);
        ValueMetaInterface extension = new ValueMetaString("extension");
        type.setLength(500);
        type.setPrecision(-1);
        type.setOrigin(name);
        row.addValueMeta(extension);
        ValueMetaInterface size = new ValueMetaInteger("size");
        size.setOrigin(name);
        row.addValueMeta(size);
        if (this.includeRowNumber()) {
            ValueMetaInterface v = new ValueMetaInteger(space.environmentSubstitute(this.getRowNumberField()));
            v.setLength(10, 0);
            v.setOrigin(name);
            row.addValueMeta(v);
        }
    }

    public SharepointFileWrapper<MS365GetFileNamesMeta> getFileWrapper(ISharepointConnection iSharepointConnection, LogChannelInterface logChannelInterface){
        SharepointFileWrapper<MS365GetFileNamesMeta> sharepointFileWrapper = new SharepointFileWrapper<>(this, iSharepointConnection, logChannelInterface);
        sharepointFileWrapper.addIStreamProvider(this.getFileName(), this.getFileMask(), this.getExcludeFileMask());
        return sharepointFileWrapper;
    }
}
