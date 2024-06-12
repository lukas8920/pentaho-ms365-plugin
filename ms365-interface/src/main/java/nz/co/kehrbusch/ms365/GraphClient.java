package nz.co.kehrbusch.ms365;

import com.azure.identity.ClientSecretCredential;
import com.azure.identity.ClientSecretCredentialBuilder;
import com.microsoft.graph.serviceclient.GraphServiceClient;

public class GraphClient {
    private GraphServiceClient graphServiceClient;

    public GraphClient(String clientId, String password, String tenantId, String[] scope){
        // Authenticate with Azure AD using the Client Secret Credential
        ClientSecretCredential clientSecretCredential = new ClientSecretCredentialBuilder()
                .clientId(clientId)
                .clientSecret(password)
                .tenantId(tenantId)
                .build();

        // Create a GraphServiceClient with the AuthProvider
        this.graphServiceClient = new GraphServiceClient(clientSecretCredential, scope);
    }

    public boolean isConnected(){
        try {
            graphServiceClient.sites().get();
        } catch (Exception e){
            return false;
        }
        return true;
    }
}
