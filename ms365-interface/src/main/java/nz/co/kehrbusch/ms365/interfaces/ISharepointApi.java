package nz.co.kehrbusch.ms365.interfaces;

import com.microsoft.graph.models.*;

import java.io.InputStream;

public interface ISharepointApi {
    SiteCollectionResponse getAllSites(int maxNrOfResults);
    DriveCollectionResponse getDrivesBySiteId(String siteId, int maxNrOfResults);
    DriveItem getRootItemByDrivId(String driveId);
    InputStream getInputStreamByDriveIdAndItemId(String driveId, String itemId);
    DriveItemCollectionResponse getItemsByDriveIdAndItemId(String driveId, String itemId, int maxNrOfResults);
    void updateDriveItemByDriveIdAndItemId(String driveId, String itemId, InputStream inputStream);
    DriveItem createNewItemByDriveIdandParentIdandName(String driveId, String parentId, DriveItem driveItem);
}
