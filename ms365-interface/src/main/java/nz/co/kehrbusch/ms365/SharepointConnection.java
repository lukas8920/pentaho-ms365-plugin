package nz.co.kehrbusch.ms365;

import com.azure.identity.ClientSecretCredential;
import com.azure.identity.ClientSecretCredentialBuilder;
import com.microsoft.graph.models.*;
import com.microsoft.graph.serviceclient.GraphServiceClient;
import nz.co.kehrbusch.ms365.interfaces.IGraphClientDetails;
import nz.co.kehrbusch.ms365.interfaces.ISharepointApi;
import nz.co.kehrbusch.ms365.interfaces.ISharepointConnection;
import nz.co.kehrbusch.ms365.interfaces.entities.Counter;
import nz.co.kehrbusch.ms365.interfaces.entities.ICountableSharepointFile;
import nz.co.kehrbusch.ms365.interfaces.entities.ISharepointFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.InvalidPathException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import java.util.stream.Collectors;

class SharepointConnection implements ISharepointConnection {
    private static final int MAX_SITES_TO_FETCH = 100;

    private static final Logger log = Logger.getLogger(SharepointConnection.class.getName());
    private static final int MAX_REQUEST_COUNTER = 50;

    private final IGraphClientDetails iGraphClientDetails;
    private final ISharepointApi iSharepointApi;

    SharepointConnection(IGraphClientDetails iGraphClientDetails){
        this.iGraphClientDetails = iGraphClientDetails;
        // Authenticate with Azure AD using the Client Secret Credential
        ClientSecretCredential clientSecretCredential = new ClientSecretCredentialBuilder()
                .clientId(iGraphClientDetails.getClientId())
                .clientSecret(iGraphClientDetails.getPassword())
                .tenantId(iGraphClientDetails.getTenantId())
                .build();

        // Create a GraphServiceClient with the AuthProvider
        GraphServiceClient graphServiceClient = new GraphServiceClient(clientSecretCredential, iGraphClientDetails.getScope());
        this.iSharepointApi = new SharepointApi(graphServiceClient);
    }

    @Override
    public List<ISharepointFile> getSites(int maxNrOfResults){
        try {
            List<ISharepointFile> iSharepointFiles = new ArrayList<>();
            int top = Math.max(maxNrOfResults, 100);
            SiteCollectionResponse siteCollectionResponse = this.iSharepointApi.getAllSites(top);
            List<Site> sites = siteCollectionResponse.getValue();
            if (sites.size() == 0) return iSharepointFiles;

            sites.stream().filter(site -> site.getName() != null).forEach(site -> {
                SharepointObject siteFile = new SharepointObject(site.getId(), site.getName(), null);
                iSharepointFiles.add(siteFile);

                DriveCollectionResponse driveCollectionResponse = this.iSharepointApi.getDrivesBySiteId(site.getId(), top);
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
            this.iGraphClientDetails.logError("Could not retrieve sites from server - returning empty list.");
            e.printStackTrace();
        }
        return new ArrayList<>();
    }

    @Override
    public List<ISharepointFile> getRootItems(ISharepointFile parent, int maxNrOfResults){
        try {
            DriveItem rootItem = this.iSharepointApi.getRootItemByDrivId(parent.getId());
            SharepointObject rootFile = new SharepointObject(rootItem.getId(), rootItem.getName(), parent);

            return getChildren(rootFile, parent, maxNrOfResults);
        } catch (Exception e){
            this.iGraphClientDetails.logError("Could not retrieve root items from server - returning empty list");
            e.printStackTrace();
        }
        return new ArrayList<>();
    }

    @Override
    public List<ISharepointFile> getChildren(ISharepointFile parent, int maxNrOfResults){
        try {
            return this.getChildren(parent, parent, maxNrOfResults);
        } catch (Exception e){
            this.iGraphClientDetails.logError("Could not retrieve children from server - returning empty list");
            e.printStackTrace();
        }
        return new ArrayList<>();
    }

    @Override
    public InputStream getInputStream(ISharepointFile iSharepointFile) throws IOException {
        ISharepointFile driveFile = determineDriveFile(iSharepointFile);
        String driveId = driveFile.getId();
        String fileId = iSharepointFile.getId();
        this.iGraphClientDetails.logBasic("Get input stream for: ");
        this.iGraphClientDetails.logBasic("Drive id: " + driveId);
        this.iGraphClientDetails.logBasic("File id: " + fileId);
        try {
            return this.iSharepointApi.getInputStreamByDriveIdAndItemId(driveId, fileId);
        } catch (Exception e){
            throw new IOException("Could not get Input Stream from server.");
        }
    }

    @Override
    public ISharepointFile inflateTreeByPath(String path) throws InvalidPathException {
        String[] parts = path.split("/");
        List<ISharepointFile> resultFiles = new ArrayList<>();

        this.iGraphClientDetails.logDebug("Try to identify path provided by user");
        //sharepoint + site + filename + empty space
        final Counter counter = new Counter(parts.length - 4);
        List<ISharepointFile> iSharepointFiles = getSites(MAX_SITES_TO_FETCH);
        List<ISharepointFile> drives = iSharepointFiles.stream()
                .filter(iSharepointFile -> iSharepointFile.getName().equals(parts[3])).collect(Collectors.toList());
        if (drives.isEmpty()) throw new InvalidPathException("No matching drive found.", "User Input");
        this.iGraphClientDetails.logDebug("Filtered drives: ");
        drives.forEach(drive -> this.iGraphClientDetails.logDebug("Drive name: " + drive.getName()));

        List<ISharepointFile> rootItems = new ArrayList<>();
        drives.forEach(drive -> {
            ((ICountableSharepointFile) drive).setFileCounter(counter.copy());
            List<ISharepointFile> items = getMatchingRootItems((ICountableSharepointFile) drive, parts);
            rootItems.addAll(items);
            if (((ICountableSharepointFile) drive).getFileCounter().getCount() <= 0) resultFiles.addAll(items);
        });
        if (rootItems.isEmpty()) throw new InvalidPathException("No matching root item found.", "User Input");
        if (!resultFiles.isEmpty()) return resultFiles.get(0);
        this.iGraphClientDetails.logDebug("Filtered root items: ");
        rootItems.forEach(item -> this.iGraphClientDetails.logDebug("Root item: " + item.getName()));

        Counter part = new Counter(5);
        rootItems.forEach(rootItem -> {
            ((ICountableSharepointFile) rootItem).setPartCounter(part.copy());
            ((ICountableSharepointFile) rootItem).setFileCounter(((ICountableSharepointFile) rootItem.getParentObject()).getFileCounter());

            validateChildItems(rootItem, resultFiles, parts);
        });

        if (resultFiles.isEmpty()) throw new InvalidPathException("No matching item found.", "User Input");
        return resultFiles.get(0);
    }

    private void validateChildItems(ISharepointFile parent, List<ISharepointFile> resultFiles, String[] parts){
        List<ISharepointFile> items = getChildren(parent, parent.getChildrenCount());
        items.forEach(item -> this.iGraphClientDetails.logDebug("Sub Directory: " + item.getName()));
        this.iGraphClientDetails.logDebug("Compare against: " + parts[((ICountableSharepointFile) parent).getPartCounter().getCount()]);

        ((ICountableSharepointFile) parent).getFileCounter().decrement(1);

        List<ISharepointFile> childItems = items.stream()
                .filter(iSharepointFile -> iSharepointFile.getName().equals(parts[((ICountableSharepointFile) parent).getPartCounter().getCount()])).collect(Collectors.toList());

        ((ICountableSharepointFile) parent).getPartCounter().increment(1);

        childItems.forEach(childItem -> {
            ((ICountableSharepointFile) childItem).setFileCounter(((ICountableSharepointFile) parent).getFileCounter().copy());
            ((ICountableSharepointFile) childItem).setPartCounter(((ICountableSharepointFile) parent).getPartCounter().copy());
        });

        if (checkCounter(childItems) && !childItems.isEmpty()){
            resultFiles.addAll(childItems);
        } else if (!childItems.isEmpty()){
            childItems.forEach(file -> validateChildItems(file, resultFiles, parts));
        }
    }

    private boolean checkCounter(List<ISharepointFile> filesToCheck){
        return filesToCheck.stream().anyMatch(iSharepointFile -> ((ICountableSharepointFile) iSharepointFile).getFileCounter().getCount() <= 0);
    }

    private List<ISharepointFile> getMatchingRootItems(ICountableSharepointFile drive, String[] parts){
        List<ISharepointFile> rootDirectory = getRootItems(drive, MAX_SITES_TO_FETCH);
        drive.getFileCounter().decrement(1);
        return rootDirectory.stream()
                .filter(iSharepointFile -> drive.getFileCounter().getCount() <= 0 ? iSharepointFile.getName().equals(parts[parts.length - 1]) : iSharepointFile.getName().equals(parts[4])).collect(Collectors.toList());
    }

    //Responsibility of the caller to add child
    private List<ISharepointFile> getChildren(ISharepointFile physicalParent, ISharepointFile virtualParent, int maxNrOfResults) {
        List<ISharepointFile> iSharepointFiles = new ArrayList<>();
        int top = Math.max(maxNrOfResults, 100);
        ISharepointFile driveFile = determineDriveFile(physicalParent);

        DriveItemCollectionResponse items = this.iSharepointApi.getItemsByDriveIdAndItemId(driveFile.getId(), physicalParent.getId(), top);

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
