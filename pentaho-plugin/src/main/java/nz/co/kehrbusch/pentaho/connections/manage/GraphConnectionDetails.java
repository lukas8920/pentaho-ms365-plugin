package nz.co.kehrbusch.pentaho.connections.manage;

import nz.co.kehrbusch.ms365.GraphClientModule;
import nz.co.kehrbusch.ms365.interfaces.IGraphClientDetails;
import nz.co.kehrbusch.ms365.interfaces.IGraphConnection;
import nz.co.kehrbusch.ms365.interfaces.ISharepointConnection;
import org.pentaho.di.core.encryption.KettleTwoWayPasswordEncoder;
import org.pentaho.di.core.logging.LogChannelInterface;
import org.pentaho.di.i18n.BaseMessages;
import org.pentaho.support.encryption.PasswordEncoderException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.util.logging.Logger;

import static nz.co.kehrbusch.pentaho.connections.util.StringHelper.isEmpty;

public class GraphConnectionDetails implements ConnectionDetailsInterface, IGraphClientDetails {
    private static final Class<?> PKG = GraphConnectionDetails.class;

    private final GraphConnectionType graphConnectionType;
    private final KettleTwoWayPasswordEncoder encoder;

    private ISharepointConnection iSharepointConnection;

    private String connectionName = "";
    private String description = "";

    private String scope = "";
    private String tenantId = "";
    private String clientId = "";
    private String password = "";

    private boolean hasDuplicateConnectionName = false;

    public GraphConnectionDetails(GraphConnectionType graphConnectionType){
        this.graphConnectionType = graphConnectionType;
        this.scope = graphConnectionType.getScopeUrl();
        this.encoder = new KettleTwoWayPasswordEncoder();
        try {
            this.encoder.init();
        } catch (PasswordEncoderException e) {
            logError(e.getMessage());
        }
    }

    @Override
    public ConnectionTypeInterface provideType() {
        return this.graphConnectionType;
    }

    @Override
    public String getConnectionName() {
        return this.connectionName;
    }

    @Override
    public String getDescription() {
        return this.description;
    }

    @Override
    public void setConnectionName(String connectionName){
        this.connectionName = connectionName;
    }

    @Override
    public void setDescription(String description){
        this.description = description;
    }

    @Override
    public void setHasDuplicateConnectionName(boolean flag) {
        this.hasDuplicateConnectionName = flag;
    }

    @Override
    public boolean hasDuplicateConnectionName() {
        return this.hasDuplicateConnectionName;
    }

    @Override
    public void writeFileXMLConnection(Document doc, Element element) {
        element.setAttribute("name", this.connectionName);

        // Create and append <scope> element
        Element scopeElement = doc.createElement("scope");
        scopeElement.appendChild(doc.createTextNode(this.scope));
        element.appendChild(scopeElement);

        // Create and append <type> element
        Element typeElement = doc.createElement("type");
        typeElement.appendChild(doc.createTextNode(this.graphConnectionType.provideType()));
        element.appendChild(typeElement);

        // Create and append <password> element
        Element passwordElement = doc.createElement("password");
        passwordElement.appendChild(doc.createTextNode(this.encoder.encode(this.password)));
        element.appendChild(passwordElement);

        // Create and append <clientId> element
        Element clientId = doc.createElement("clientid");
        clientId.appendChild(doc.createTextNode(this.clientId));
        element.appendChild(clientId);

        // Create and append <clientId> element
        Element tenantId = doc.createElement("tenantid");
        tenantId.appendChild(doc.createTextNode(this.tenantId));
        element.appendChild(tenantId);

        // Create and append <clientId> element
        Element descriptionElement = doc.createElement("description");
        descriptionElement.appendChild(doc.createTextNode(this.description));
        element.appendChild(descriptionElement);
    }

    @Override
    public GraphConnectionDetails clone(){
        GraphConnectionDetails graphConnectionDetails = new GraphConnectionDetails(this.graphConnectionType);
        graphConnectionDetails.setConnectionName(this.connectionName);
        graphConnectionDetails.setTenantId(this.tenantId);
        graphConnectionDetails.setPassword(this.password);
        graphConnectionDetails.setDescription(this.description);
        graphConnectionDetails.setClientId(this.clientId);
        graphConnectionDetails.setScope(this.scope);
        return graphConnectionDetails;
    }

    @Override
    public void readFileXMLProperties(Element element){
        // Extract name, scope, and password
        this.connectionName = element.getAttribute("name");
        this.scope = element.getElementsByTagName("scope").item(0).getTextContent();
        this.password = encoder.decode(element.getElementsByTagName("password").item(0).getTextContent());
        this.clientId = element.getElementsByTagName("clientid").item(0).getTextContent();
        this.tenantId = element.getElementsByTagName("tenantid").item(0).getTextContent();
        this.description = element.getElementsByTagName("description").item(0).getTextContent();
        this.iSharepointConnection = GraphClientModule.provideSharepointConnection(this);
    }

    public void setScope(String scope){
        this.scope = scope;
    }

    public void setTenantId(String tenantId){
        this.tenantId = tenantId;
    }

    public void setClientId(String clientId){
        this.clientId = clientId;
    }

    public void setPassword(String password){
        this.password = password;
    }

    public String getScope(){
        return this.scope;
    }

    public String getTenantId(){
        return this.tenantId;
    }

    public String getClientId(){
        return this.clientId;
    }

    public String getPassword(){
        return this.password;
    }

    public ISharepointConnection getISharepointConnection(){
        return this.iSharepointConnection;
    }

    public void setiSharepointConnection(){
        this.iSharepointConnection = GraphClientModule.provideSharepointConnection(this);
    }

    @Override
    public ConnectionDetailsInterface copyToNewDetailsInterface(ConnectionTypeInterface newTypeInterface) {
        ConnectionDetailsInterface detailsInterface = this;
        if (!newTypeInterface.equals(graphConnectionType)){
            detailsInterface = newTypeInterface.createInstanceOfDetails();
            detailsInterface.setConnectionName(this.connectionName);
            detailsInterface.setDescription(this.description);
            ((GraphConnectionDetails) detailsInterface).setScope(this.scope);
            ((GraphConnectionDetails) detailsInterface).setPassword(this.password);
            ((GraphConnectionDetails) detailsInterface).setClientId(this.clientId);
            ((GraphConnectionDetails) detailsInterface).setTenantId(this.tenantId);
        }
        return detailsInterface;
    }

    @Override
    public UITypeCallback provideCallback() {
        return new GraphConnectionType.ChangeCallback() {
            @Override
            public void updateScope(String scope) {
                GraphConnectionDetails.this.scope = scope;
            }

            @Override
            public void updateTenantId(String tenantId) {
                GraphConnectionDetails.this.tenantId = tenantId;
            }

            @Override
            public void updateClientId(String clientId) {
                GraphConnectionDetails.this.clientId = clientId;
            }

            @Override
            public void updatePassword(String password) {
                GraphConnectionDetails.this.password = password;
            }
        };
    }

    @Override
    public String toString(){
        return this.connectionName;
    }

    public String computeValidateMessage(){
        if ( isEmpty(this.connectionName) ) {
            return BaseMessages.getString( PKG, "GraphConnectionDetails.validate.failure.noName" );
        }
        if ( isEmpty(this.scope)) {
            return BaseMessages.getString( PKG, "GraphConnectionDetails.validate.failure.noScope" );
        }
        if ( isEmpty(this.tenantId) ) {
            return BaseMessages.getString( PKG, "GraphConnectionDetails.validate.failure.noTenantId" );
        }
        if ( isEmpty(this.clientId) ) {
            return BaseMessages.getString( PKG, "GraphConnectionDetails.validate.failure.noClientId" );
        }
        if ( isEmpty(this.password) ) {
            return BaseMessages.getString( PKG, "GraphConnectionDetails.validate.failure.noPassword" );
        }
        return "";
    }

    @Override
    public boolean test() {
        IGraphConnection iGraphConnection = GraphClientModule.provideGraphConnection(this);
        return iGraphConnection.isConnected();
    }

    @Override
    public void onInitiatedByUser() {
        this.setiSharepointConnection();
    }

    @Override
    public void logBasic(String message) {
        this.graphConnectionType.getLog().logBasic(message);
    }

    @Override
    public void logError(String message) {
        this.graphConnectionType.getLog().logError(message);
    }

    @Override
    public void logDebug(String message) {
        this.graphConnectionType.getLog().logDebug(message);
    }
}
