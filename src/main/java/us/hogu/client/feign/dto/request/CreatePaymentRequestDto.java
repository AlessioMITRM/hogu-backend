package us.hogu.client.feign.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreatePaymentRequestDto {
    
    @NotNull(message = "L'importo è obbligatorio")
    @DecimalMin(value = "0.01", message = "L'importo deve essere maggiore di 0")
    private Double amount;
    
    @NotBlank(message = "La valuta è obbligatoria")
    private String currency;
    
    @NotBlank(message = "Il metodo di pagamento è obbligatorio")
    private String paymentMethodId;
    
    private String customerEmail;
    
    private String description;
    
    @Builder.Default
    private Map<String, String> metadata = new HashMap<>();
    
    
    // Metodi utility
    public void addMetadata(String key, String value) {
        if (this.metadata == null) {
            this.metadata = new HashMap<>();
        }
        this.metadata.put(key, value);
    }
    
    public String getMetadataValue(String key) {
        return this.metadata != null ? this.metadata.get(key) : null;
    }
    
    // Metodo factory per casi d'uso comuni
    public static CreatePaymentRequestDto createBasicPayment(Double amount, String currency, String paymentMethodId) {
        return CreatePaymentRequestDto.builder()
            .amount(amount)
            .currency(currency)
            .paymentMethodId(paymentMethodId)
            .build();
    }
    
    public static CreatePaymentRequestDto createPaymentWithMetadata(Double amount, String currency, 
                                                                   String paymentMethodId, Map<String, String> metadata) {
        return CreatePaymentRequestDto.builder()
            .amount(amount)
            .currency(currency)
            .paymentMethodId(paymentMethodId)
            .metadata(metadata != null ? metadata : new HashMap<>())
            .build();
    }
}