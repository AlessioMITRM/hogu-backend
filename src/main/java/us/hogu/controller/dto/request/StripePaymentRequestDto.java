package us.hogu.controller.dto.request;

import javax.validation.constraints.NotBlank;

import lombok.Data;

@Data
public class StripePaymentRequestDto extends PaymentRequestDto {
    @NotBlank
	private String paymentIdIntent;
    
    @NotBlank
    private String customerId;
}
