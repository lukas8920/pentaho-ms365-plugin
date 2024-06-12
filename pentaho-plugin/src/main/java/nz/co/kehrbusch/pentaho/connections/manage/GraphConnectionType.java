package nz.co.kehrbusch.pentaho.connections.manage;

import nz.co.kehrbusch.pentaho.connections.ui.dialog.MS365DetailsCompositeHelper;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.pentaho.di.ui.core.PropsUI;

public class GraphConnectionType implements ConnectionTypeInterface {
    private static final String TYPE = "Microsoft Graph API";
    private static final Class<?> PKG = GraphConnectionType.class;
    private static final int TEXT_VAR_FLAGS = SWT.SINGLE | SWT.LEFT | SWT.BORDER;
    private static final String SCOPE_URL = "https://graph.microsoft.com/.default";

    private static GraphConnectionType graphConnectionType = null;

    private final MS365DetailsCompositeHelper helper;

    private Text scopeText;
    private Text tenantIdText;
    private Text clientIdText;
    private Text textVar;
    private Label title;
    private Label scopeLabel;
    private Label tenantIdLabel;
    private Label clientIdLabel;
    private Label clientSecretLabel;

    private GraphConnectionType(){
        PropsUI props = PropsUI.getInstance();
        this.helper = new MS365DetailsCompositeHelper(PKG, props);
    }

    public static GraphConnectionType getInstance(){
        if (graphConnectionType == null){
            graphConnectionType = new GraphConnectionType();
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
    public GraphConnectionTypeComposite openDialog(Display display, UITypeCallback changeCallback, Composite wTabFolder, int width) {
        title = this.helper.createTitle(display, wTabFolder, SWT.CENTER | SWT.FILL, "ConnectionDialog.Graph.Title", null);
        scopeLabel = this.helper.createLabel(wTabFolder, SWT.LEFT | SWT.WRAP, "ConnectionDialog.Graph.Scope", title, 4);
        scopeText = this.helper.createText(wTabFolder, TEXT_VAR_FLAGS, scopeLabel, 0, 4);
        scopeText.setText(SCOPE_URL);
        tenantIdLabel = this.helper.createLabel(wTabFolder, SWT.LEFT | SWT.WRAP, "ConnectionDialog.Graph.TenantId", scopeText, 4);
        tenantIdText = this.helper.createText(wTabFolder, TEXT_VAR_FLAGS, tenantIdLabel, 0, 4);
        clientIdLabel = this.helper.createLabel(wTabFolder, SWT.LEFT | SWT.WRAP, "ConnectionDialog.Graph.ClientId", tenantIdText, 4);
        clientIdText = this.helper.createText(wTabFolder, TEXT_VAR_FLAGS, clientIdLabel, 0, 4);
        clientSecretLabel = this.helper.createLabel(wTabFolder, SWT.LEFT | SWT.WRAP, "ConnectionDialog.Graph.ClientSecret", clientIdText, 4);
        textVar = this.helper.createText(wTabFolder, TEXT_VAR_FLAGS, clientSecretLabel, 0, 4);
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
    }

    @Override
    public void insertUIValues(ConnectionDetailsInterface connectionDetailsInterface) {
        GraphConnectionDetails graphConnectionDetails = (GraphConnectionDetails) connectionDetailsInterface;
        this.scopeText.setText(graphConnectionDetails.getScope());
        this.textVar.setText(graphConnectionDetails.getPassword());
        this.clientIdText.setText(graphConnectionDetails.getClientId());
        this.tenantIdText.setText(graphConnectionDetails.getTenantId());
    }

    public class GraphConnectionTypeComposite {
        public GraphConnectionTypeComposite(ChangeCallback changeCallback){
            GraphConnectionType.this.scopeText.addModifyListener(modifyEvent -> {
                changeCallback.updateScope(scopeText.getText());
            });
            GraphConnectionType.this.tenantIdText.addModifyListener(modifyEvent -> changeCallback.updateTenantId(tenantIdText.getText()));
            GraphConnectionType.this.clientIdText.addModifyListener(modifyEvent -> changeCallback.updateClientId(clientIdText.getText()));
            GraphConnectionType.this.textVar.addModifyListener(modifyEvent -> changeCallback.updatePassword(textVar.getText()));
        }
    }

    public interface ChangeCallback extends UITypeCallback {
        void updateScope(String scope);
        void updateTenantId(String tenantId);
        void updateClientId(String clientId);
        void updatePassword(String password);
    }
}
