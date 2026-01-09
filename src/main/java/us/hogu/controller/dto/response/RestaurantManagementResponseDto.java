package us.hogu.controller.dto.response;

import lombok.Builder;
import lombok.Data;
import java.time.OffsetDateTime;

@Data
@Builder
public class RestaurantManagementResponseDto {
    private Long id;
    
    private String name;
    
    private Boolean publicationStatus;
    
    private OffsetDateTime creationDate;
    
    private Long userId;
}
