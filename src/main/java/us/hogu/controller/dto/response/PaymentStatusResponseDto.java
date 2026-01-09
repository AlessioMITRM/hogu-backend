package us.hogu.controller.dto.response;

import javax.validation.constraints.NotBlank;

import lombok.Data;
import us.hogu.model.enums.PaymentStatus;

@Data
public class PaymentStatusResponseDto {
	@NotBlank
	private PaymentStatus status;
    
    @NotBlank
    private String paymentIdIntent;
    
    @NotBlank
    private String clientSecret; // Per Stripe
}
