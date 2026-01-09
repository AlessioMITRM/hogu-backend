package us.hogu.controller.dto.request;

import javax.validation.constraints.NotBlank;

import lombok.Data;
import us.hogu.model.enums.ServiceType;

@Data
public class ServiceCreateRequestDto {
	@NotBlank
    private ServiceType serviceType; // RESTAURANT, NCC, CLUB, LUGGAGE
    
	@NotBlank
    private Object serviceData; // DTO specifico del servizio
}
