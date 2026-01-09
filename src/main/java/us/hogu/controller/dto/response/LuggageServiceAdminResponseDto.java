package us.hogu.controller.dto.response;

import java.time.OffsetDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LuggageServiceAdminResponseDto {
	
	private Long id;
	
    private String name;
    
    private Boolean publicationStatus;
    
    private OffsetDateTime creationDate;
    
    private Integer capacity;
    
    private String providerName;

}
