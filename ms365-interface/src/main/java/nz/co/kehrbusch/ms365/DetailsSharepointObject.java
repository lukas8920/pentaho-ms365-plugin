package nz.co.kehrbusch.ms365;

import com.microsoft.graph.models.DriveItem;
import nz.co.kehrbusch.ms365.interfaces.entities.ICountableSharepointFile;
import nz.co.kehrbusch.ms365.interfaces.entities.IDetailsSharepointFile;
import nz.co.kehrbusch.ms365.interfaces.entities.ISharepointFile;

import java.time.LocalDateTime;

public class DetailsSharepointObject extends SharepointObject implements IDetailsSharepointFile {
    private final LocalDateTime lastModifiedDate;
    private final String webUrl;
    private final LocalDateTime createdDate;

    public DetailsSharepointObject(DriveItem driveItem, ISharepointFile parent){
        super(driveItem, parent);

        this.setSize(Integer.parseInt(String.valueOf(driveItem.getSize())));
        this.lastModifiedDate = driveItem.getLastModifiedDateTime().toLocalDateTime();
        this.webUrl = driveItem.getWebUrl();
        this.createdDate = driveItem.getCreatedDateTime().toLocalDateTime();
    }

    @Override
    public LocalDateTime getModifiedDate() {
        return this.lastModifiedDate;
    }

    @Override
    public String getWebUrl() {
        return this.webUrl;
    }

    @Override
    public LocalDateTime getCreatedDate() {
        return this.createdDate;
    }
}
