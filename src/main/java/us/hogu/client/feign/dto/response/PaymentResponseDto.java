package us.hogu.client.feign.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import us.hogu.model.enums.PaymentStatus;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentResponseDto {
    
	private PaymentStatus paymentStatus;
    
    private String paymentIdIntent;
    
    private String clientSecret;
    
    private Double amount;
    
    private String currency;
    
    private String errorMessage;
}
