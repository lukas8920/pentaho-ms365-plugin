package nz.co.kehrbusch.ms365.util;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.microsoft.aad.msal4j.*;
import nz.co.kehrbusch.ms365.interfaces.IGraphClientDetails;

import java.net.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class SiteIdentifier {
    private static final String baseAuthority = "https://login.microsoftonline.com/";
    private static final String baseScope = "api://%s/.default";

    private final IGraphClientDetails iGraphClientDetails;

    public SiteIdentifier(IGraphClientDetails iGraphClientDetails){
        this.iGraphClientDetails = iGraphClientDetails;
    }

    public List<String> getSiteIds(){
        try {
            ConfidentialClientApplication app;
            if (!iGraphClientDetails.getProxyHost().isEmpty() && !iGraphClientDetails.getProxyPort().isEmpty()){
                try {
                    app = generateClientWithProxy();
                } catch (Exception e) {
                    iGraphClientDetails.logError("Proxy Error during set up of GraphServiceClient.");
                    return new ArrayList<>();
                }
            } else {
                app = generateClientWithoutProxy();
            }
            ClientCredentialParameters clientCredentialParam = ClientCredentialParameters.builder(Collections.singleton(String.format(baseScope, iGraphClientDetails.getClientId()))).build();
            IAuthenticationResult result = app.acquireToken(clientCredentialParam).join();
            String accessToken = result.accessToken();
            return decodeJwt(accessToken);
        } catch (Exception e){
            this.iGraphClientDetails.logError("Error during fetching site ids.");
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    private ConfidentialClientApplication generateClientWithProxy() throws MalformedURLException {
        int port = Integer.parseInt(iGraphClientDetails.getProxyPort());
        Proxy proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress(iGraphClientDetails.getProxyHost(), port));
        // Define the proxy authenticator
        if (!iGraphClientDetails.getProxyUser().isEmpty()){
            Authenticator.setDefault(new Authenticator() {
                @Override
                protected PasswordAuthentication getPasswordAuthentication() {
                    return new PasswordAuthentication(iGraphClientDetails.getProxyUser(), iGraphClientDetails.getProxyPassword().toCharArray());
                }
            });
        }

        return ConfidentialClientApplication.builder(iGraphClientDetails.getClientId(), ClientCredentialFactory.createFromSecret(iGraphClientDetails.getPassword()))
                .authority(baseAuthority + iGraphClientDetails.getTenantId())
                .proxy(proxy)
                .build();
    }

    private ConfidentialClientApplication generateClientWithoutProxy() throws MalformedURLException {
        return ConfidentialClientApplication.builder(iGraphClientDetails.getClientId(), ClientCredentialFactory.createFromSecret(iGraphClientDetails.getPassword()))
                .authority(baseAuthority + iGraphClientDetails.getTenantId())
                .build();
    }

    private List<String> decodeJwt(String jwt) {
        List<String> siteIds = new ArrayList<>();
        String[] splitToken = jwt.split("\\.");
        String base64EncodedHeader = splitToken[0];
        String base64EncodedBody = splitToken[1];

        String body = new String(java.util.Base64.getUrlDecoder().decode(base64EncodedBody));

        JsonParser parser = new JsonParser();
        JsonObject jsonObject = parser.parse(body).getAsJsonObject();

        if (jsonObject.has("sites")) {
            String sites = jsonObject.get("sites").getAsString();
            if (sites.contains("[") && sites.contains("]")){
                siteIds.addAll(parseList(sites));
            } else {
                siteIds.add(sites);
            }
        } else {
            System.out.println("No site claim found in token.");
            return new ArrayList<>();
        }

        return siteIds;
    }

    private List<String> parseList(String rawList){
        String[] items = rawList.substring(1, rawList.length() - 1).replace("\"", "").split(",");
        return Arrays.asList(items);
    }
}
