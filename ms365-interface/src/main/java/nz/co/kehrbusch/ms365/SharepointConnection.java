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

    public static void main(String[] args) {
        SharepointConnection sharepointConnection = new SharepointConnection(new IGraphClientDetails() {
            @Override
            public String getScope() {
                return "https://graph.microsoft.com/.default";
            }

            @Override
            public String getTenantId() {
                return "c72ef424-5d1d-47c0-9a32-27e5e76d49d5";
            }

            @Override
            public String getClientId() {
                return "c901f8d5-8425-4ef1-8538-585deaabac47";
            }

            @Override
            public String getPassword() {
                return "uje8Q~KYda.waGAbhPuHAeK70oUTc1Pf9d_C~cwO";
            }
        });
        List<ISharepointFile> iSharepointFiles = sharepointConnection.getSites(50);
        List<ISharepointFile> sites = getSites(iSharepointFiles);
        sites.forEach(site -> {
            System.out.println(site.getName());
            site.getChildren().forEach(drive -> {
                System.out.println(" - " + drive.getName());
                List<ISharepointFile> children = sharepointConnection.getRootItems(drive, 50);
                children.forEach(child -> System.out.println(" -- " + child.getName()));
            });
        });
    }

    private static List<ISharepointFile> getSites(List<ISharepointFile> iSharepointFiles){
        return iSharepointFiles.stream().filter(iSharepointFile -> iSharepointFile.getParentObject() == null)
                .collect(Collectors.toList());
    }

    @Override
    public List<ISharepointFile> getSites(int maxNrOfResults){
        List<ISharepointFile> iSharepointFiles = new ArrayList<>();
        int top = Math.max(maxNrOfResults, 100);
        SiteCollectionResponse siteCollectionResponse = this.graphServiceClient.sites().get(config -> {
            config.queryParameters.top = top;
        });
        List<Site> sites = siteCollectionResponse.getValue();
        if (sites.size() == 0) return iSharepointFiles;

        sites.stream().filter(site -> site.getName() != null).forEach(site -> {
            SharepointObject siteFile = new SharepointObject(site.getId(), site.getName(), null, true);
            iSharepointFiles.add(siteFile);

            DriveCollectionResponse driveCollectionResponse = this.graphServiceClient
                    .sites().bySiteId(site.getId())
                    .drives().get(config -> {
                        config.queryParameters.top = top;
                    });
            List<Drive> drives = driveCollectionResponse.getValue();
            if (drives.size() == 0) return;

            drives.forEach(drive -> {
                SharepointObject driveFile = new SharepointObject(drive.getId(), drive.getName(), siteFile, true);
                siteFile.addChild(driveFile);
                iSharepointFiles.add(driveFile);
            });
        });
        return iSharepointFiles;
    }

    @Override
    public List<ISharepointFile> getRootItems(ISharepointFile parent, int maxNrOfResults){
        DriveItem rootItem = this.graphServiceClient.drives()
                .byDriveId(parent.getId())
                .root().get();
        SharepointObject rootFile = new SharepointObject(rootItem.getId(), rootItem.getName(), parent, false);

        return getChildren(rootFile, parent, maxNrOfResults);
    }

    @Override
    public List<ISharepointFile> getChildren(ISharepointFile parent, int maxNrOfResults){
        return this.getChildren(parent, parent, maxNrOfResults);
    }

    private List<ISharepointFile> getChildren(ISharepointFile physicalParent, ISharepointFile virtualParent, int maxNrOfResults) {
        List<ISharepointFile> iSharepointFiles = new ArrayList<>();
        int top = Math.max(maxNrOfResults, 100);
        ISharepointFile driveFile = determineDriveFile(physicalParent);

        DriveItemCollectionResponse items = this.graphServiceClient.drives().byDriveId(driveFile.getId())
                .items().byDriveItemId(physicalParent.getId()).children().get(config ->
                        config.queryParameters.top = top);
        List<DriveItem> driveItems = items.getValue();
        if (driveItems.size() != 0) {
            ((SharepointObject) physicalParent).setHasChildren(true);
            driveItems.forEach(driveItem -> {
                Folder folder = driveItem.getFolder();
                int childrenCount = folder != null ? folder.getChildCount() : 0;
                ISharepointFile item = new SharepointObject(driveItem.getId(), driveItem.getName(), virtualParent, childrenCount);
                ((SharepointObject) virtualParent).addChild(item);
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
