package us.hogu.controller.dto.request;

import javax.validation.constraints.NotBlank;

import io.micrometer.core.lang.NonNull;
import lombok.Data;
import us.hogu.model.enums.ServiceType;

@Data
public class BookingCreateRequestDto {
	@NotBlank
    private ServiceType serviceType; // RESTAURANT, NCC, CLUB, LUGGAGE
    
	@NonNull
    private Long serviceId;
    
	@NonNull
    private Object bookingData; // DTO specifico del servizio
}
