package nz.co.kehrbusch.ms365;

import com.azure.identity.ClientSecretCredential;
import com.azure.identity.ClientSecretCredentialBuilder;
import com.microsoft.graph.models.DriveItem;
import com.microsoft.graph.models.DriveItemCollectionResponse;
import com.microsoft.graph.models.File;
import com.microsoft.graph.models.Folder;
import com.microsoft.graph.serviceclient.GraphServiceClient;
import nz.co.kehrbusch.ms365.interfaces.IGraphClientDetails;
import nz.co.kehrbusch.ms365.interfaces.ISharepointApi;
import nz.co.kehrbusch.ms365.interfaces.entities.ICountableSharepointFile;
import nz.co.kehrbusch.ms365.interfaces.entities.ISharepointFile;

import java.nio.file.InvalidPathException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public abstract class SharepointBaseProcessor {
    protected static final int MAX_SUPPORTED_PATH_DEPTH = 10;
    protected static final int MAX_SITES_TO_FETCH = 100;

    protected final ISharepointApi iSharepointApi;
    protected final IGraphClientDetails iGraphClientDetails;

    public abstract List<ISharepointFile> getChildren(ISharepointFile parent, int maxNrOfResults);
    public abstract List<ISharepointFile> getRootItems(ISharepointFile parent, int maxNrOfResults);

    public SharepointBaseProcessor(IGraphClientDetails iGraphClientDetails){
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

    protected void validateChildItems(ISharepointFile parent, List<ISharepointFile> resultFiles, String[] parts){
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
        } else {
            //trigger creation process for folders + file
            ISharepointFile drive = determineDriveFile(parent);
            int count = ((ICountableSharepointFile) parent).getPartCounter().getCount() - 1;
            ISharepointFile iSharepointFile = this.createPath(drive, parent, parent, Arrays.copyOfRange(parts, count, parts.length));
            resultFiles.add(iSharepointFile);
        }
    }

    protected boolean checkCounter(List<ISharepointFile> filesToCheck){
        return filesToCheck.stream().anyMatch(iSharepointFile -> ((ICountableSharepointFile) iSharepointFile).getFileCounter().getCount() <= 0);
    }

    protected ISharepointFile getRootItem(ISharepointFile parent){
        DriveItem rootItem = this.iSharepointApi.getRootItemByDrivId(parent.getId());
        return new SharepointObject(rootItem.getId(), rootItem.getName(), parent);
    }

    protected List<ISharepointFile> getMatchingRootItems(ICountableSharepointFile drive, String[] parts){
        List<ISharepointFile> rootDirectory = getRootItems(drive, MAX_SITES_TO_FETCH);
        drive.getFileCounter().decrement(1);
        return rootDirectory.stream()
                .filter(iSharepointFile -> drive.getFileCounter().getCount() <= 0 ? iSharepointFile.getName().equals(parts[parts.length - 1]) : iSharepointFile.getName().equals(parts[4])).collect(Collectors.toList());
    }

    //Responsibility of the caller to add child
    protected List<ISharepointFile> getChildren(ISharepointFile physicalParent, ISharepointFile virtualParent, int maxNrOfResults) {
        List<ISharepointFile> iSharepointFiles = new ArrayList<>();
        int top = Math.max(maxNrOfResults, 100);
        ISharepointFile driveFile = determineDriveFile(physicalParent);

        DriveItemCollectionResponse items = this.iSharepointApi.getItemsByDriveIdAndItemId(driveFile.getId(), physicalParent.getId(), top);

        List<DriveItem> driveItems = items.getValue();
        if (driveItems.size() != 0) {
            driveItems.forEach(driveItem -> {
                Folder folder = driveItem.getFolder();
                //set to at least 1 if instance of folder
                int childrenCount = folder != null ? (folder.getChildCount() == 0 ? 1 : folder.getChildCount()) : 0;
                ISharepointFile item = new SharepointObject(driveItem.getId(), driveItem.getName(), virtualParent, childrenCount);
                iSharepointFiles.add(item);
            });
        }
        return iSharepointFiles;
    }

    protected ISharepointFile determineDriveFile(ISharepointFile iSharepointFile){
        ISharepointFile parent = iSharepointFile.getParentObject();
        while (parent != null && parent.getParentObject() != null && parent.getParentObject().getParentObject() != null){
            parent = parent.getParentObject();
        }
        return parent;
    }

    protected ISharepointFile createRootPath(ISharepointFile drive, String[] path){
        DriveItem rootItem = this.iSharepointApi.getRootItemByDrivId(drive.getId());
        SharepointObject rootFile = new SharepointObject(rootItem.getId(), rootItem.getName(), drive);
        return createPath(drive, rootFile, drive, Arrays.copyOfRange(path, 4, path.length));
    }

    protected ISharepointFile createPath(ISharepointFile drive, ISharepointFile physicalParent, ISharepointFile virtualParent, String[] path) throws InvalidPathException {
        int length = path.length;
        if (length > MAX_SUPPORTED_PATH_DEPTH) throw new InvalidPathException("Path is invalid as input", "User Input");
        ISharepointFile mVirtualParent = virtualParent;
        ISharepointFile mPhysicalParent = physicalParent;
        for (int i = 0; i < (path.length); i++){
            String part = path[i];
            if (i == (path.length - 1)){
                this.iGraphClientDetails.logBasic("Create file: " + part);
                //last item is a file
                DriveItem driveItem = new DriveItem();
                driveItem.setName(part);
                driveItem.setFile(new File());

                DriveItem createdFile = this.iSharepointApi.createNewItemByDriveIdandParentIdandName(drive.getId(), mPhysicalParent.getId(), driveItem);
                mPhysicalParent = new SharepointObject(createdFile, mVirtualParent);
            } else {
                this.iGraphClientDetails.logBasic("Create folder: " + part);
                DriveItem driveItem = new DriveItem();
                driveItem.setName(part);
                driveItem.setFolder(new Folder());
                DriveItem createdFile = this.iSharepointApi.createNewItemByDriveIdandParentIdandName(drive.getId(), mPhysicalParent.getId(), driveItem);
                mPhysicalParent = new SharepointObject(createdFile, mVirtualParent);
            }
            mVirtualParent = mPhysicalParent;
        }
        return mPhysicalParent;
    }
}
