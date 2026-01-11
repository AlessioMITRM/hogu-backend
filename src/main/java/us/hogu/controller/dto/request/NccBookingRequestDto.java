package us.hogu.controller.dto.request;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

import us.hogu.controller.dto.response.RestaurantServiceResponseDto;
import us.hogu.controller.dto.response.ServiceDetailResponseDto;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;

@Data
public class NccBookingRequestDto {
	@NotNull
	private long nccServiceId;
	
	@NotNull
    private OffsetDateTime pickupTime;
    
	@NotBlank
    private String pickupLocation;
    
	@NotBlank
    private String destination;
	
    @NotNull
    private BigDecimal totalAmount;
}
