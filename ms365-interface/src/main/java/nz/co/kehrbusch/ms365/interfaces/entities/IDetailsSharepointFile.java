package nz.co.kehrbusch.ms365.interfaces.entities;

import java.time.LocalDateTime;

public interface IDetailsSharepointFile extends ISharepointFile {
    LocalDateTime getModifiedDate();
    String getWebUrl();
    LocalDateTime getCreatedDate();
}
