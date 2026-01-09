package us.hogu.controller.dto.response;

import java.util.List;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.experimental.Accessors;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
@EqualsAndHashCode(callSuper = true)
public class NccServiceResponseDto extends ServiceDetailResponseDto {
    
    private List<VehicleResponseDto> vehiclesAvailable;
    
    private List<ServiceLocaleResponseDto> locales;    
}
