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

class SharepointConnection implements ISharepointConnection {
    private static final Logger log = Logger.getLogger(SharepointConnection.class.getName());

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
    public List<ISharepointFile> getDrives(int maxNrOfResults) {
        List<ISharepointFile> iSharepointFiles = new ArrayList<>();
        int top = Math.max(maxNrOfResults, 100);
        DriveCollectionResponse driveCollectionResponse = this.graphServiceClient.drives().get(config -> {
            config.queryParameters.top = top;
        });
        List<Drive> drives = driveCollectionResponse.getValue();
        log.info("Retrieved drive information, size: " + drives.size());
        if (drives.size() == 0) return iSharepointFiles;

        drives.forEach(drive -> {
            SharepointObject driveFile = new SharepointObject(drive.getId(), drive.getName(), null, true);
            iSharepointFiles.add(driveFile);

            DriveItem rootItem = this.graphServiceClient.drives()
                    .byDriveId(drive.getId())
                    .root().get();
            SharepointObject rootFile = new SharepointObject(rootItem.getId(), rootItem.getName(), driveFile, false);
            driveFile.addChild(rootFile);
            iSharepointFiles.add(rootFile);
            log.info("Retrieved root file");

            List<ISharepointFile> driveItems = getChildren(rootFile, maxNrOfResults);
            iSharepointFiles.addAll(driveItems);
        });
        return iSharepointFiles;
    }

    @Override
    public List<ISharepointFile> getChildren(ISharepointFile parentFile, int maxNrOfResults) {
        List<ISharepointFile> iSharepointFiles = new ArrayList<>();
        int top = Math.max(maxNrOfResults, 100);
        ISharepointFile driveFile = determineDriveFile(parentFile);

        DriveItemCollectionResponse items = this.graphServiceClient.drives().byDriveId(driveFile.getId())
                .items().byDriveItemId(parentFile.getId()).children().get(config ->
                        config.queryParameters.top = top);
        List<DriveItem> driveItems = items.getValue();
        if (driveItems.size() != 0) {
            ((SharepointObject) parentFile).setHasChildren(true);
            driveItems.forEach(driveItem -> {
                ISharepointFile item = new SharepointObject(driveItem.getId(), driveItem.getName(), parentFile, false);
                ((SharepointObject) parentFile).addChild(item);
                iSharepointFiles.add(item);
            });
        }
        log.info("Retrieved drive items");
        return iSharepointFiles;
    }

    private ISharepointFile determineDriveFile(ISharepointFile iSharepointFile){
        ISharepointFile parent = iSharepointFile.getParentObject();
        while (parent != null){
            parent = iSharepointFile.getParentObject();
        }
        return parent;
    }
}
