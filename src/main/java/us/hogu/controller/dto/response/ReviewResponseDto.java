package us.hogu.controller.dto.response;

import java.time.OffsetDateTime;

import lombok.Data;
import us.hogu.model.enums.ServiceType;

@Data
public class ReviewResponseDto {
    private Long id;
    
    private Long userId;
    
    private ServiceType serviceType;
    
    private Long serviceId;
    
    private Integer rating;
    
    private String reviewText;
    
    private OffsetDateTime creationDate;
    
    private UserResponseDto user;
}
