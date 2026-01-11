package us.hogu.controller.dto.response;

import java.math.BigDecimal;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LuggageSizePriceResponseDto {
    private Long id;
    private String sizeLabel;       // SMALL, MEDIUM, LARGE, EXTRA_LARGE
    private BigDecimal pricePerHour;
    private BigDecimal pricePerDay;
    private String description;     // es. "fino a 15 kg"
}

