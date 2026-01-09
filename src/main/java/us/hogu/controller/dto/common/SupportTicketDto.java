package us.hogu.controller.dto.common;

import java.time.OffsetDateTime;

import lombok.Data;

@Data
public class SupportTicketDto {
    private Long id;
    
    private Long userId;
    
    private String userName;
    
    private String userEmail;
    
    private String subject;
    
    private String message;
    
    private String status; // OPEN, IN_PROGRESS, RESOLVED, CLOSED
    
    private String priority; // LOW, MEDIUM, HIGH, URGENT
    
    private OffsetDateTime creationDate;

    private OffsetDateTime lastUpdateDate;
    
    private String adminResponse;
    
    private Long assignedAdminId;
    
    private String assignedAdminName;
}
