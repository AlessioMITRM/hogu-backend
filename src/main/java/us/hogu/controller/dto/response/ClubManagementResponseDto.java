package us.hogu.controller.dto.response;

import java.math.BigDecimal;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ClubManagementResponseDto {

    private Long id;

    private String name;

    private String description;
    
    private List<ServiceLocaleResponseDto> locales;
    
    private Long maxCapacity;
    
    private BigDecimal basePrice;

    private List<String> images;
    
    private Boolean publicationStatus;
}
