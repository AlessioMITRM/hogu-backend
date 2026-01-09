package us.hogu.client.feign.dto.request;

import java.util.HashMap;
import java.util.Map;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdatePaymentRequestDto {
    private Double amount;
    private String currency;
    private String description;
    private Map<String, String> metadata;
    
}
