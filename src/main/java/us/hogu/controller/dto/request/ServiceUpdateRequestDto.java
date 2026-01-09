package us.hogu.controller.dto.request;

import javax.validation.constraints.NotBlank;

import lombok.Data;
import us.hogu.model.enums.ServiceType;

@Data
public class ServiceUpdateRequestDto {
	@NotBlank
    private Long id;
    
	@NotBlank
    private ServiceType serviceType;
    
	@NotBlank
    private Object serviceData;
}
