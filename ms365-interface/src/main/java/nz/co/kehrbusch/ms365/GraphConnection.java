package nz.co.kehrbusch.ms365;

import com.azure.core.http.HttpClient;
import com.azure.core.http.ProxyOptions;
import com.azure.core.http.netty.NettyAsyncHttpClientBuilder;
import com.azure.identity.ClientSecretCredential;
import com.azure.identity.ClientSecretCredentialBuilder;
import com.microsoft.graph.core.requests.GraphClientFactory;
import com.microsoft.graph.serviceclient.GraphServiceClient;
import com.microsoft.kiota.authentication.AuthenticationProvider;
import com.microsoft.kiota.authentication.BaseBearerTokenAuthenticationProvider;
import nz.co.kehrbusch.ms365.interfaces.IGraphClientDetails;
import nz.co.kehrbusch.ms365.interfaces.IGraphConnection;
import nz.co.kehrbusch.ms365.util.AzureIdentityAccessTokenProvider;
import okhttp3.Authenticator;
import okhttp3.Credentials;
import okhttp3.OkHttpClient;

import java.net.InetSocketAddress;
import java.net.Proxy;
import java.util.Map;

class GraphConnection implements IGraphConnection {
    private GraphServiceClient graphServiceClient;

    GraphConnection(IGraphClientDetails iGraphClientDetails){
        if (!iGraphClientDetails.getProxyHost().isEmpty() && !iGraphClientDetails.getProxyPort().isEmpty()){
            try {
                this.graphServiceClient = generateClientWithProxy(iGraphClientDetails);
            } catch (Exception e) {
                iGraphClientDetails.logError("Proxy Error during set up of GraphServiceClient.");
            }
        } else {
            this.graphServiceClient = generateClientWithoutProxy(iGraphClientDetails);
        }
    }

    static GraphServiceClient generateClientWithoutProxy(IGraphClientDetails iGraphClientDetails){
        // Authenticate with Azure AD using the Client Secret Credential
        ClientSecretCredential clientSecretCredential = new ClientSecretCredentialBuilder()
                .clientId(iGraphClientDetails.getClientId())
                .clientSecret(iGraphClientDetails.getPassword())
                .tenantId(iGraphClientDetails.getTenantId())
                .build();

        // Create a GraphServiceClient with the AuthProvider
        return new GraphServiceClient(clientSecretCredential, iGraphClientDetails.getScope());
    }

    static GraphServiceClient generateClientWithProxy(IGraphClientDetails iGraphClientDetails) throws Exception {
        int port = Integer.parseInt(iGraphClientDetails.getProxyPort());

        final InetSocketAddress proxyAddress = new InetSocketAddress(iGraphClientDetails.getProxyHost(),
                port);
        final ProxyOptions options = new ProxyOptions(ProxyOptions.Type.HTTP, proxyAddress);
        if (!iGraphClientDetails.getProxyUser().isEmpty()){
            options.setCredentials(iGraphClientDetails.getProxyUser(), iGraphClientDetails.getProxyPassword());
        }
        final HttpClient authClient = new NettyAsyncHttpClientBuilder().proxy(options)
                .build();

        final ClientSecretCredential credential = new ClientSecretCredentialBuilder()
                .clientId(iGraphClientDetails.getClientId())
                .tenantId(iGraphClientDetails.getTenantId())
                .clientSecret(iGraphClientDetails.getPassword())
                .httpClient(authClient)
                .build();

        if (credential == null) {
            throw new Exception("Could not create credential");
        }

        String[] allowedHosts = new String[] { "*" };
        AuthenticationProvider provider = new BaseBearerTokenAuthenticationProvider(new AzureIdentityAccessTokenProvider(credential, allowedHosts, null, iGraphClientDetails.getScope()));

        final Proxy proxy = new Proxy(Proxy.Type.HTTP, proxyAddress);

        final Authenticator proxyAuthenticator = (route, response) -> {
            if (!iGraphClientDetails.getProxyUser().isEmpty()){
                String credential1 = Credentials.basic(iGraphClientDetails.getProxyUser(), iGraphClientDetails.getProxyPassword());
                return response.request().newBuilder()
                        .header("Proxy-Authorization", credential1).build();
            } else {
                return response.request().newBuilder().build();
            }
        };

        final OkHttpClient httpClient = GraphClientFactory.create().proxy(proxy)
                .proxyAuthenticator(proxyAuthenticator).build();
        return new GraphServiceClient(provider, httpClient);
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
