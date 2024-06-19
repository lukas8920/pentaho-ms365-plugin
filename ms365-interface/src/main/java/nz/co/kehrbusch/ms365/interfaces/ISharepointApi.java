package nz.co.kehrbusch.ms365.interfaces;

import com.microsoft.graph.models.DriveCollectionResponse;
import com.microsoft.graph.models.DriveItem;
import com.microsoft.graph.models.DriveItemCollectionResponse;
import com.microsoft.graph.models.SiteCollectionResponse;

import java.io.InputStream;

public interface ISharepointApi {
    SiteCollectionResponse getAllSites(int maxNrOfResults);
    DriveCollectionResponse getDrivesBySiteId(String siteId, int maxNrOfResults);
    DriveItem getRootItemByDrivId(String driveId);
    InputStream getInputStreamByDriveIdAndItemId(String driveId, String itemId);
    DriveItemCollectionResponse getItemsByDriveIdAndItemId(String driveId, String itemId, int maxNrOfResults);
}
