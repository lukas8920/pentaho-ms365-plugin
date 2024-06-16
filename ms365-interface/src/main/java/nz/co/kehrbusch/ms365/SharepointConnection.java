package nz.co.kehrbusch.ms365;

import com.azure.identity.ClientSecretCredential;
import com.azure.identity.ClientSecretCredentialBuilder;
import com.microsoft.graph.models.*;
import com.microsoft.graph.serviceclient.GraphServiceClient;
import nz.co.kehrbusch.ms365.interfaces.IGraphClientDetails;
import nz.co.kehrbusch.ms365.interfaces.ISharepointConnection;
import nz.co.kehrbusch.ms365.interfaces.entities.ISharepointFile;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import java.util.stream.Collectors;

class SharepointConnection implements ISharepointConnection {
    private static final Logger log = Logger.getLogger(SharepointConnection.class.getName());
    private static final int MAX_REQUEST_COUNTER = 50;

    private final GraphServiceClient graphServiceClient;

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
    public List<ISharepointFile> getSites(int maxNrOfResults){
        try {
            List<ISharepointFile> iSharepointFiles = new ArrayList<>();
            int top = Math.max(maxNrOfResults, 100);
            SiteCollectionResponse siteCollectionResponse = this.graphServiceClient.sites().get(config -> {
                config.queryParameters.top = top;
            });
            List<Site> sites = siteCollectionResponse.getValue();
            if (sites.size() == 0) return iSharepointFiles;

            sites.stream().filter(site -> site.getName() != null).forEach(site -> {
                SharepointObject siteFile = new SharepointObject(site.getId(), site.getName(), null);
                iSharepointFiles.add(siteFile);

                DriveCollectionResponse driveCollectionResponse = this.graphServiceClient
                        .sites().bySiteId(site.getId())
                        .drives().get(config -> {
                            config.queryParameters.top = top;
                        });
                List<Drive> drives = driveCollectionResponse.getValue();
                if (drives.size() == 0) return;

                drives.forEach(drive -> {
                    SharepointObject driveFile = new SharepointObject(drive.getId(), drive.getName(), siteFile);
                    siteFile.addChild(driveFile);
                    iSharepointFiles.add(driveFile);
                });
            });
            return iSharepointFiles;
        } catch (Exception e){
            log.warning("Could not retrieve sites from server - returning empty list.");
            e.printStackTrace();
        }
        return new ArrayList<>();
    }

    @Override
    public List<ISharepointFile> getRootItems(ISharepointFile parent, int maxNrOfResults){
        try {
            DriveItem rootItem = this.graphServiceClient.drives()
                    .byDriveId(parent.getId())
                    .root().get();
            SharepointObject rootFile = new SharepointObject(rootItem.getId(), rootItem.getName(), parent);

            return getChildren(rootFile, parent, maxNrOfResults);
        } catch (Exception e){
            log.warning("Could not retrieve root items from server - returning empty list");
            e.printStackTrace();
        }
        return new ArrayList<>();
    }

    @Override
    public List<ISharepointFile> getChildren(ISharepointFile parent, int maxNrOfResults){
        try {
            return this.getChildren(parent, parent, maxNrOfResults);
        } catch (Exception e){
            log.warning("Could not retrieve children from server - returning empty list");
            e.printStackTrace();
        }
        return new ArrayList<>();
    }

    //Responsibility of the caller to add child
    private List<ISharepointFile> getChildren(ISharepointFile physicalParent, ISharepointFile virtualParent, int maxNrOfResults) {
        List<ISharepointFile> iSharepointFiles = new ArrayList<>();
        int top = Math.max(maxNrOfResults, 100);
        ISharepointFile driveFile = determineDriveFile(physicalParent);

        DriveItemCollectionResponse items = this.graphServiceClient.drives().byDriveId(driveFile.getId())
                .items().byDriveItemId(physicalParent.getId()).children().get(config ->
                        config.queryParameters.top = top);
        List<DriveItem> driveItems = items.getValue();
        if (driveItems.size() != 0) {
            driveItems.forEach(driveItem -> {
                Folder folder = driveItem.getFolder();
                int childrenCount = folder != null ? folder.getChildCount() : 0;
                ISharepointFile item = new SharepointObject(driveItem.getId(), driveItem.getName(), virtualParent, childrenCount);
                iSharepointFiles.add(item);
            });
        }
        return iSharepointFiles;
    }

    private ISharepointFile determineDriveFile(ISharepointFile iSharepointFile){
        ISharepointFile parent = iSharepointFile.getParentObject();
        while (parent.getParentObject() != null && parent.getParentObject().getParentObject() != null){
            parent = parent.getParentObject();
        }
        return parent;
    }
}
