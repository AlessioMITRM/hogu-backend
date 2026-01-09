package us.hogu.client.feign.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import us.hogu.model.enums.ServiceType;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StripePaymentRequestDto {
	
    private Double amount;
    
    private String currency;
    
    private String paymentIdMethod;
    
    private Long bookingId;
    
    private Long userId;
    
    private ServiceType serviceType;
    
    private String description;
    
    private String customerEmail;
}
