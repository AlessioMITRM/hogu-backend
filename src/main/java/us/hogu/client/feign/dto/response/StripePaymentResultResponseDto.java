package us.hogu.client.feign.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class StripePaymentResultResponseDto {
    private boolean success;
    private String paymentIntentId;
    private String clientSecret;
    private Double amount;
    private String currency;
    private String errorMessage;
}
