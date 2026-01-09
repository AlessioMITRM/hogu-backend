package us.hogu.controller.dto.response;

import java.time.OffsetDateTime;

import lombok.Data;

@Data
public class NotificationResponseDto {
    private Long id;
    
    private Long userId;
    
    private String title;
    
    private String message;
    
    private Boolean read;
    
    private OffsetDateTime creationDate;
}
