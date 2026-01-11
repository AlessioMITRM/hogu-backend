package us.hogu.controller.dto.request;

import java.math.BigDecimal;
import java.util.List;

import lombok.Data;

@Data
public class NccServiceRequestDto {
		
    private String name;
    
    private String description;
    
    private VehicleRequestDto vehicle;
    
    private BigDecimal basePrice;
    
    private List<ServiceLocaleRequestDto> locales;
            
    private Boolean publicationStatus;
    
    private List<String> images;
}
