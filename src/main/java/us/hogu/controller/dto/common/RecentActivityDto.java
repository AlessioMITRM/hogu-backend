package us.hogu.controller.dto.common;

import java.time.OffsetDateTime;

import lombok.Data;

@Data
public class RecentActivityDto {
    private String action; // APPROVED_PROVIDER, CREATED_SERVICE, UPDATED_BOOKING, etc.
    
    private String entity; // USER, SERVICE, BOOKING, PAYMENT, etc.
    
    private Long entityId;
    
    private String adminName;
    
    private String adminEmail;
    
    private OffsetDateTime timestamp;
    
    private String details;
}
