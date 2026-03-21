package us.hogu.controller.dto.request;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

import lombok.Data;

@Data
public class ClubBookingRequestDto {
	
	@NotNull
	private Long eventId;

	@NotNull
	@Min(1)
	private Long clubServiceId;
	
	@NotNull
    private OffsetDateTime reservationTime;
    
	@NotNull
	private Integer numberOfPeople;
    	
    private String billingFirstName;
    
    private String billingLastName;

	@NotBlank
    private String specialRequests;

	private ServiceLocaleRequestDto locale;

	private String fiscalCode;

	private String taxId;

    private EventPricingConfigurationRequestDto pricingConfiguration;
}
