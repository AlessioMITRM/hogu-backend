package us.hogu.controller.dto.request;

import java.time.OffsetDateTime;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

import lombok.Data;

@Data
public class LuggageBookingRequestDto {
	@NotNull
	private long luggageServiceId;
	
	@NotNull
    private OffsetDateTime reservationTime;
    
	@NotNull
    private Integer numberOfPeople;
    
	@NotBlank
    private String specialRequests;
	
	@NotNull
	private double totalAmount;
}
