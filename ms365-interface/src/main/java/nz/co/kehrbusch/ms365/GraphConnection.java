package nz.co.kehrbusch.ms365;

import com.azure.identity.ClientSecretCredential;
import com.azure.identity.ClientSecretCredentialBuilder;
import com.microsoft.graph.serviceclient.GraphServiceClient;
import nz.co.kehrbusch.ms365.interfaces.IGraphClientDetails;
import nz.co.kehrbusch.ms365.interfaces.IGraphConnection;

class GraphConnection implements IGraphConnection {
    private GraphServiceClient graphServiceClient;

    GraphConnection(IGraphClientDetails iGraphClientDetails){
        // Authenticate with Azure AD using the Client Secret Credential
        ClientSecretCredential clientSecretCredential = new ClientSecretCredentialBuilder()
                .clientId(iGraphClientDetails.getClientId())
                .clientSecret(iGraphClientDetails.getPassword())
                .tenantId(iGraphClientDetails.getTenantId())
                .build();

        // Create a GraphServiceClient with the AuthProvider
        this.graphServiceClient = new GraphServiceClient(clientSecretCredential, iGraphClientDetails.getScope());
    }

    @Override
    public boolean isConnected() {
        try {
            graphServiceClient.sites().get();
        } catch (Exception e){
            return false;
        }
        return true;
    }
}
