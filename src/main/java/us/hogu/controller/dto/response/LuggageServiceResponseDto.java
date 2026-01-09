package us.hogu.controller.dto.response;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)

public class LuggageServiceResponseDto extends ServiceDetailResponseDto {
    private Integer capacity;
    
    private List<LuggageSizePriceResponseDto> sizePrices;
    
    private List<OpeningHourResponseDto> openingHours;
    
    private List<ServiceLocaleResponseDto> locales;
}
