package us.hogu.repository.projection;

import java.time.OffsetDateTime;

import us.hogu.model.enums.UserRole;

public interface UserProfileProjection {
    Long getId();
    
    String getName();
    
    String getSurname();
    
    String getEmail();
    
    UserRole getRole();
    
    OffsetDateTime getCreationDate();
}
