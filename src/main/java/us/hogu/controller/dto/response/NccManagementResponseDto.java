package us.hogu.controller.dto.response;

import java.time.OffsetDateTime;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class NccManagementResponseDto {
    Long id;
    
    String name;
    
    Boolean publicationStatus;
    
    OffsetDateTime creationDate;
}
