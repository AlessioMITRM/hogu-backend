package us.hogu.repository.projection;

import java.time.OffsetDateTime;

public interface NccManagementProjection {
    Long getId();
    
    String getName();
    
    Boolean getPublicationStatus();
    
    OffsetDateTime getCreationDate();
}
