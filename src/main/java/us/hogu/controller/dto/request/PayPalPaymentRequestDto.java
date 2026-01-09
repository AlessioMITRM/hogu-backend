package us.hogu.controller.dto.request;

import javax.validation.constraints.NotBlank;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class PayPalPaymentRequestDto extends PaymentRequestDto {
    @NotBlank
	private String orderId;
    
    @NotBlank
    private String PayerId;
}
