package nz.co.kehrbusch.ms365;

import com.microsoft.graph.models.*;
import com.microsoft.graph.serviceclient.GraphServiceClient;
import nz.co.kehrbusch.ms365.interfaces.ISharepointApi;

import java.io.InputStream;

public class SharepointApi implements ISharepointApi {
    private final GraphServiceClient graphServiceClient;

    public SharepointApi(GraphServiceClient graphServiceClient){
        this.graphServiceClient = graphServiceClient;
    }

    @Override
    public SiteCollectionResponse getAllSites(int maxNrOfResults) {
        return this.graphServiceClient.sites().get(config -> {
            config.queryParameters.top = maxNrOfResults;
        });
    }

    @Override
    public DriveCollectionResponse getDrivesBySiteId(String siteId, int maxNrOfResults) {
        return this.graphServiceClient
                .sites().bySiteId(siteId)
                .drives().get(config -> {
                    config.queryParameters.top = maxNrOfResults;
                });
    }

    @Override
    public DriveItem getRootItemByDrivId(String driveId) {
        return this.graphServiceClient.drives()
                .byDriveId(driveId)
                .root().get();
    }

    @Override
    public InputStream getInputStreamByDriveIdAndItemId(String driveId, String itemId) {
        return this.graphServiceClient.drives().byDriveId(driveId).items().byDriveItemId(itemId).content().get();
    }

    @Override
    public DriveItemCollectionResponse getItemsByDriveIdAndItemId(String driveId, String itemId, int maxNrOfResults) {
        return this.graphServiceClient.drives().byDriveId(driveId)
                .items().byDriveItemId(itemId).children().get(config ->
                        config.queryParameters.top = maxNrOfResults);
    }

    @Override
    public void updateDriveItemByDriveIdAndItemId(String driveId, String itemId, InputStream inputStream) {
        this.graphServiceClient.drives().byDriveId(driveId).items().byDriveItemId(itemId).content().put(inputStream);
    }

    @Override
    public DriveItem createNewItemByDriveIdandParentIdandName(String driveId, String parentId, DriveItem driveItem) {
        return this.graphServiceClient.drives().byDriveId(driveId).items().byDriveItemId(parentId).children().post(driveItem);
    }

    @Override
    public Site getSiteById(String siteId) {
        return this.graphServiceClient.sites().bySiteId(siteId).get();
    }
}
