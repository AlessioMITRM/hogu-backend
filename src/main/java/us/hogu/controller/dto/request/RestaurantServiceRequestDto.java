package us.hogu.controller.dto.request;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import lombok.Data;
import us.hogu.model.internal.Menu;

@Data
public class RestaurantServiceRequestDto {
    private String name;
    
    private String description;
    
    private List<ServiceLocaleRequestDto> locales;
    
    private String menu;
    
    private Integer capacity;
    
    private BigDecimal basePrice;
    
    private List<String> images;
    
    private Boolean publicationStatus;
}
