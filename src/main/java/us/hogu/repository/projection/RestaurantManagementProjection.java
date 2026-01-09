package us.hogu.repository.projection;

import java.time.OffsetDateTime;

public interface RestaurantManagementProjection {
    Long getId();
    
    String getName();
    
    Boolean getPublicationStatus();
    
    OffsetDateTime getCreationDate();
    
    Long getUserId();
}
