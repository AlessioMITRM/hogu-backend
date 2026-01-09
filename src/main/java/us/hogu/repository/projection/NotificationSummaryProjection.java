package us.hogu.repository.projection;

import java.time.OffsetDateTime;

public interface NotificationSummaryProjection {
    Long getId();
    
    String getTitle();
    
    String getMessage();
    
    Boolean getRead();
    
    OffsetDateTime getCreationDate();
}
