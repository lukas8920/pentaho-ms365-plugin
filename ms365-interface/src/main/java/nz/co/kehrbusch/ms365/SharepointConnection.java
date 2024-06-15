package nz.co.kehrbusch.ms365;

import com.azure.identity.ClientSecretCredential;
import com.azure.identity.ClientSecretCredentialBuilder;
import com.microsoft.graph.serviceclient.GraphServiceClient;
import nz.co.kehrbusch.ms365.interfaces.IGraphClientDetails;
import nz.co.kehrbusch.ms365.interfaces.ISharepointConnection;
import nz.co.kehrbusch.ms365.interfaces.entities.ISharepointFile;

import java.util.List;

class SharepointConnection implements ISharepointConnection {
    private GraphServiceClient graphServiceClient;

    SharepointConnection(IGraphClientDetails iGraphClientDetails){
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
    public List<ISharepointFile> getDrives(boolean maxNrOfResults) {
        return null;
    }

    @Override
    public List<ISharepointFile> getRootFiles(boolean maxNrOfResuls) {
        return null;
    }

    @Override
    public List<ISharepointFile> getChildren(ISharepointFile iSharepointFile, boolean maxNrOfResults) {
        return null;
    }
}
