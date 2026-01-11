package us.hogu.controller.dto.request;

import java.math.BigDecimal;

import javax.validation.constraints.NotBlank;

import io.micrometer.core.lang.NonNull;
import lombok.Data;
import lombok.NoArgsConstructor;
import us.hogu.model.enums.PaymentMethod;

@Data
@NoArgsConstructor
public class PaymentRequestDto {
	@NonNull
	private Long bookingId;
    
	@NonNull
	private BigDecimal amount;
    
	@NotBlank
    private String currency;
    
    @NotBlank
    private PaymentMethod paymentMethod; // STRIPE, PAYPAL
}
