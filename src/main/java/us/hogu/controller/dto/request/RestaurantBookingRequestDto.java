package us.hogu.controller.dto.request;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

import lombok.Data;

@Data
public class RestaurantBookingRequestDto {
	@NotNull
	@Min(1)
	private Long restaurantServiceId;
	
	@NotNull
	private OffsetDateTime reservationTime;
    
    @NotNull
    private Integer numberOfPeople;
    
    private String billingFirstName;
    
    private String billingLastName;
    
    private String billingAddress;
    
    @NotBlank
    private String billingEmail;

    private String fiscalCode;
    
    private String taxId;

    private String specialRequests;
    
    private BigDecimal totalAmount;
}
