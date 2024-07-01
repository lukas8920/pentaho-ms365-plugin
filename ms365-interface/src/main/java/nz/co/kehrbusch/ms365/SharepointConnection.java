package nz.co.kehrbusch.ms365;

import com.microsoft.graph.models.*;
import nz.co.kehrbusch.ms365.interfaces.IGraphClientDetails;
import nz.co.kehrbusch.ms365.interfaces.ISharepointConnection;
import nz.co.kehrbusch.ms365.interfaces.entities.Counter;
import nz.co.kehrbusch.ms365.interfaces.entities.ICountableSharepointFile;
import nz.co.kehrbusch.ms365.interfaces.entities.ISharepointFile;
import nz.co.kehrbusch.ms365.util.SiteIdentifier;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.InvalidPathException;
import java.util.*;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Logger;
import java.util.stream.Collectors;

class SharepointConnection extends SharepointBaseProcessor implements ISharepointConnection {
    private static final Logger log = Logger.getLogger(SharepointConnection.class.getName());
    private static final int MAX_REQUEST_COUNTER = 50;

    SharepointConnection(IGraphClientDetails iGraphClientDetails){
        super(iGraphClientDetails);
    }

    @Override
    public List<ISharepointFile> getSites(int maxNrOfResults){
        try {
            List<ISharepointFile> iSharepointFiles = new ArrayList<>();
            int top = Math.max(maxNrOfResults, 100);
            SiteCollectionResponse siteCollectionResponse = this.iSharepointApi.getAllSites(top);
            List<Site> sites = siteCollectionResponse.getValue();
            log.info("Retrieved sites from server");
            if (sites.size() == 0) {
                SiteIdentifier siteIdentifier = new SiteIdentifier(this.iGraphClientDetails);
                List<String> siteIds = siteIdentifier.getSiteIds();
                log.info("Sites are restricted - Available sites: ");
                log.info(siteIds.toString());
                if (siteIds.size() == 0){
                    return iSharepointFiles;
                }
                siteIds.forEach(siteId -> {
                    Site site = this.iSharepointApi.getSiteById(siteId);
                    if (site != null){
                        site.setName(site.getDisplayName());
                        sites.add(site);
                    }
                });
                this.iGraphClientDetails.logDebug("Added name to siteId.");
            }

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
        return this.inflateTreeByPath(path, false);
    }

    @Override
    public ISharepointFile inflateTreeByPath(String path, boolean createIfNotExists) throws InvalidPathException {
        AtomicBoolean rootFlag = new AtomicBoolean(false);
        this.iGraphClientDetails.logBasic("Inflate tree for: " + path);
        String[] tmpParts = path.split("/");
        if (tmpParts.length > 0 && tmpParts[tmpParts.length - 1].isEmpty()) {
            tmpParts = Arrays.copyOf(tmpParts, tmpParts.length - 1);
        }
        String[] parts = tmpParts;
        List<ISharepointFile> resultFiles = new ArrayList<>();

        this.iGraphClientDetails.logDebug("Try to identify path provided by user");
        //sharepoint + site + filename + empty space
        final Counter counter = new Counter(parts.length - 4);
        List<ISharepointFile> iSharepointFiles = getSites(MAX_SITES_TO_FETCH);
        List<ISharepointFile> drives = iSharepointFiles.stream()
                .filter(iSharepointFile -> iSharepointFile.getName().equals(parts[3]) && iSharepointFile.getParentObject().getName().equals(parts[2])).collect(Collectors.toList());
        if (drives.isEmpty()) throw new InvalidPathException("No matching drive found.", "User Input");
        this.iGraphClientDetails.logDebug("Filtered drives: ");
        drives.forEach(drive -> this.iGraphClientDetails.logDebug("Drive name: " + drive.getName()));

        List<ISharepointFile> rootItems = new ArrayList<>();
        drives.forEach(drive -> {
            ((ICountableSharepointFile) drive).setFileCounter(counter.copy());
            List<ISharepointFile> items = getMatchingRootItems((ICountableSharepointFile) drive, parts);
            rootItems.addAll(items);
            if (((ICountableSharepointFile) drive).getFileCounter().getCount() <= 0) resultFiles.addAll(items);
            if (parts.length == 4 && path.endsWith("/") && !rootFlag.get()){
                this.iGraphClientDetails.logDebug("Query root item of sharepoint");
                ISharepointFile root = this.getRootItem(drive);
                resultFiles.add(root);
                rootItems.add(root);
                rootFlag.set(true);
            }
        });
        if (rootItems.isEmpty() && !createIfNotExists){
            throw new InvalidPathException("No matching root item found.", "User Input");
        }
        if (rootItems.isEmpty()) return this.createRootPath(drives.get(0), parts);

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
        this.iGraphClientDetails.logDebug("Item found: " + resultFiles.get(0).getName());
        return resultFiles.get(0);
    }

    @Override
    public void writeToSharepoint(ISharepointFile iSharepointFile, byte[] data, boolean appendData) {
        InputStream inputStream = null;
        ByteArrayInputStream byteArrayInputStream = null;
        try {
            ISharepointFile drive = this.determineDriveFile(iSharepointFile);
            if (data.length > 0 && !appendData){
                this.iGraphClientDetails.logBasic("Create new file content on sharepoint.");
                byteArrayInputStream = new ByteArrayInputStream(data);
                this.iSharepointApi.updateDriveItemByDriveIdAndItemId(drive.getId(), iSharepointFile.getId(), byteArrayInputStream);
            } else if (data.length > 0){
                this.iGraphClientDetails.logBasic("Update existing file content on sharepoint.");
                inputStream = this.getInputStream(iSharepointFile);
                byte[] existingContent = ByteOperation.readInputStreamToByteArray(inputStream);
                byte[] outputContent = ByteOperation.appendByteArraysWithComparison(existingContent, data, this.iGraphClientDetails);
                byteArrayInputStream = new ByteArrayInputStream(outputContent);
                this.iSharepointApi.updateDriveItemByDriveIdAndItemId(drive.getId(), iSharepointFile.getId(), byteArrayInputStream);
            }
        } catch (Exception e){
            this.iGraphClientDetails.logError("Error writing to sharepoint.");
            e.printStackTrace();
        } finally {
            try {
                if (inputStream != null){
                    inputStream.close();
                }
                if (byteArrayInputStream != null){
                    byteArrayInputStream.close();
                }
            } catch (IOException e){
                this.iGraphClientDetails.logError("Error closing streams of writing operation.");
            }
        }

    }
}
