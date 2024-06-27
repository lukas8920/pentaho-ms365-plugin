package nz.co.kehrbusch.pentaho.connections.manage;

import nz.co.kehrbusch.pentaho.connections.ui.dialog.MS365DetailsCompositeHelper;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.*;
import org.pentaho.di.core.logging.LogChannelInterface;
import org.pentaho.di.ui.core.PropsUI;

public class GraphConnectionType implements ConnectionTypeInterface {
    private static final String TYPE = "Microsoft Graph API";
    private static final Class<?> PKG = GraphConnectionType.class;
    private static final int TEXT_VAR_FLAGS = SWT.SINGLE | SWT.LEFT | SWT.BORDER;
    private static final String SCOPE_URL = "https://graph.microsoft.com/.default";

    private static GraphConnectionType graphConnectionType = null;

    private Text scopeText;
    private Text tenantIdText;
    private Text clientIdText;
    private Text textVar;
    private Label title;
    private Label scopeLabel;
    private Label tenantIdLabel;
    private Label clientIdLabel;
    private Label clientSecretLabel;
    private Group group;
    private Label proxyHost;
    private Text proxyHostText;
    private Label proxyPort;
    private Text proxyPortText;
    private Label proxyUser;
    private Text proxyUserText;
    private Label proxyPassword;
    private Text proxyPasswordText;

    private final LogChannelInterface log;

    private GraphConnectionType(LogChannelInterface log){
        this.log = log;
    }

    public static GraphConnectionType getInstance(LogChannelInterface log){
        if (graphConnectionType == null){
            graphConnectionType = new GraphConnectionType(log);
        }
        return graphConnectionType;
    }

    @Override
    public String provideType() {
        return TYPE;
    }

    @Override
    public ConnectionDetailsInterface createInstanceOfDetails() {
        return new GraphConnectionDetails(graphConnectionType);
    }

    public String getScopeUrl(){
        return SCOPE_URL;
    }

    @Override
    public GraphConnectionTypeComposite openDialog(Display display, MS365DetailsCompositeHelper helper, UITypeCallback changeCallback, Composite wTabFolder, int width) {
        title = helper.createTitle(display, wTabFolder, SWT.CENTER | SWT.FILL, "ConnectionDialog.Graph.Title", null);
        scopeLabel = helper.createLabel(wTabFolder, SWT.LEFT | SWT.WRAP, "ConnectionDialog.Graph.Scope", title, 4);
        scopeText = helper.createText(wTabFolder, TEXT_VAR_FLAGS, scopeLabel, 0, 4);
        scopeText.setText(SCOPE_URL);
        tenantIdLabel = helper.createLabel(wTabFolder, SWT.LEFT | SWT.WRAP, "ConnectionDialog.Graph.TenantId", scopeText, 4);
        tenantIdText = helper.createText(wTabFolder, TEXT_VAR_FLAGS, tenantIdLabel, 0, 4);
        clientIdLabel = helper.createLabel(wTabFolder, SWT.LEFT | SWT.WRAP, "ConnectionDialog.Graph.ClientId", tenantIdText, 4);
        clientIdText = helper.createText(wTabFolder, TEXT_VAR_FLAGS, clientIdLabel, 0, 4);
        clientSecretLabel = helper.createLabel(wTabFolder, SWT.LEFT | SWT.WRAP, "ConnectionDialog.Graph.ClientSecret", clientIdText, 4);
        textVar = helper.createText(wTabFolder, TEXT_VAR_FLAGS, clientSecretLabel, 0, 4);
        group = helper.createGroup(wTabFolder, SWT.LEFT, "ConnectionDialog.Graph.ProxyGroup", textVar, 0, 4);
        proxyHost = helper.createLabel(group, SWT.LEFT | SWT.WRAP, "ConnectionDialog.Graph.ProxyHost", textVar, 8);
        proxyHostText = helper.createText(group, TEXT_VAR_FLAGS, proxyHost, 0, 8);
        proxyPort = helper.createLabel(group, SWT.LEFT | SWT.WRAP, "ConnectionDialog.Graph.ProxyPort", proxyHostText, 8);
        proxyPortText = helper.createText(group, TEXT_VAR_FLAGS, proxyPort, 0, 8);
        proxyUser = helper.createLabel(group, SWT.LEFT | SWT.WRAP, "ConnectionDialog.Graph.ProxyUser", proxyPortText, 8);
        proxyUserText = helper.createText(group, TEXT_VAR_FLAGS, proxyUser, 0, 8);
        proxyPassword = helper.createLabel(group, SWT.LEFT | SWT.WRAP, "ConnectionDialog.Graph.ProxySecret", proxyUserText, 8);
        proxyPasswordText = helper.createText(group, TEXT_VAR_FLAGS, proxyPassword, 0, 8);
        return new GraphConnectionTypeComposite((ChangeCallback) changeCallback);
    }

    @Override
    public void closeDialog(){
        this.title.dispose();
        this.scopeLabel.dispose();
        this.scopeText.dispose();
        this.tenantIdLabel.dispose();
        this.tenantIdText.dispose();
        this.clientIdLabel.dispose();
        this.clientIdText.dispose();
        this.clientSecretLabel.dispose();
        this.textVar.dispose();
        this.group.dispose();
        this.proxyHost.dispose();
        this.proxyHostText.dispose();
        this.proxyPort.dispose();
        this.proxyPortText.dispose();
        this.proxyUser.dispose();
        this.proxyUserText.dispose();
        this.proxyPassword.dispose();
        this.proxyPasswordText.dispose();
    }

    @Override
    public void insertUIValues(ConnectionDetailsInterface connectionDetailsInterface) {
        GraphConnectionDetails graphConnectionDetails = (GraphConnectionDetails) connectionDetailsInterface;
        this.scopeText.setText(graphConnectionDetails.getScope());
        this.textVar.setText(graphConnectionDetails.getPassword());
        this.clientIdText.setText(graphConnectionDetails.getClientId());
        this.tenantIdText.setText(graphConnectionDetails.getTenantId());
        this.proxyHostText.setText(graphConnectionDetails.getProxyHost());
        this.proxyPortText.setText(graphConnectionDetails.getProxyPort());
        this.proxyUserText.setText(graphConnectionDetails.getProxyUser());
        this.proxyPasswordText.setText(graphConnectionDetails.getProxyPassword());
    }

    @Override
    public LogChannelInterface getLog() {
        return this.log;
    }

    public class GraphConnectionTypeComposite {
        public GraphConnectionTypeComposite(ChangeCallback changeCallback){
            GraphConnectionType.this.scopeText.addModifyListener(modifyEvent -> {
                changeCallback.updateScope(scopeText.getText());
            });
            GraphConnectionType.this.tenantIdText.addModifyListener(modifyEvent -> changeCallback.updateTenantId(tenantIdText.getText()));
            GraphConnectionType.this.clientIdText.addModifyListener(modifyEvent -> changeCallback.updateClientId(clientIdText.getText()));
            GraphConnectionType.this.textVar.addModifyListener(modifyEvent -> changeCallback.updatePassword(textVar.getText()));
            GraphConnectionType.this.proxyHostText.addModifyListener(modifyEvent -> changeCallback.updateProxyHost(proxyHostText.getText()));
            GraphConnectionType.this.proxyPortText.addModifyListener(modifyEvent -> changeCallback.updateProxyPort(proxyPortText.getText()));
            GraphConnectionType.this.proxyUserText.addModifyListener(modifyEvent -> changeCallback.updateProxyUser(proxyUserText.getText()));
            GraphConnectionType.this.proxyPasswordText.addModifyListener(modifyEvent -> changeCallback.updateProxyPassword(proxyPasswordText.getText()));
        }
    }

    public interface ChangeCallback extends UITypeCallback {
        void updateScope(String scope);
        void updateTenantId(String tenantId);
        void updateClientId(String clientId);
        void updatePassword(String password);
        void updateProxyHost(String proxyHost);
        void updateProxyPort(String proxyPort);
        void updateProxyUser(String proxyUser);
        void updateProxyPassword(String proxyPassword);
    }
}
