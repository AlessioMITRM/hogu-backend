package us.hogu.controller.dto.response;

import java.math.BigDecimal;

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
    private BigDecimal price;
    private Integer capacity;
    private Boolean isActive;
}