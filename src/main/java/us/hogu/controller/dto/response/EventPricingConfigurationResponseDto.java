package us.hogu.controller.dto.response;

import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import us.hogu.model.enums.PricingType;

@Data
@Builder
public class EventPricingConfigurationResponseDto {
    private Long id;
    private PricingType pricingType;
    private String description;
    private Double price;
    private Integer capacity;
    private Boolean isActive;
}