package us.hogu.controller.dto.response;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import us.hogu.model.enums.ServiceType;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ServiceSummaryResponseDto {
    private Long id;
    
    private ServiceType serviceType;
    
    private String name;
    
    private String description;
    
    private String model;
    
    private Integer numberOfSeats;
    
    private List<ServiceLocaleResponseDto> locales;
            
    private BigDecimal basePrice;

    private BigDecimal estimatedPrice;
    
    private Double distanceKm;
    
    private List<String> images;
}
