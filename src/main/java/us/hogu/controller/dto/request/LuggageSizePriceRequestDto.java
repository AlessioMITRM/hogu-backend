package us.hogu.controller.dto.request;

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
    private Double pricePerHour;
    private Double pricePerDay;
    private String description;
}


