package us.hogu.controller.dto.request;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

import lombok.Data;

@Data
public class LuggageBookingRequestDto {
	@NotNull
	@Min(1)
	private Long luggageServiceId;
	
	@NotNull
    private OffsetDateTime dropOffTime;

	@NotNull
    private OffsetDateTime pickUpTime;

	private Integer bagsSmall;
	
	private Integer bagsMedium;
	
	private Integer bagsLarge;
    
	private String specialRequests;
	
	@NotNull
	private BigDecimal totalAmount;

    private String billingFirstName;
    
	private String billingLastName;
    
	@NotBlank
	private String billingAddress;
    
	@NotBlank
	private String billingEmail;

	private String fiscalCode;
    
	
	private String taxId;
}
