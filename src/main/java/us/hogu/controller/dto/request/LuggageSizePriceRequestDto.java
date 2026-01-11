package us.hogu.controller.dto.request;

import java.math.BigDecimal;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LuggageSizePriceRequestDto {
    private String sizeLabel;
    private BigDecimal pricePerHour;
    private BigDecimal pricePerDay;
    private String description;
}


