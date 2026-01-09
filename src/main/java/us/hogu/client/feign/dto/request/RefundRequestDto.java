package us.hogu.client.feign.dto.request;

import javax.validation.constraints.NotBlank;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class RefundRequestDto {
    @NotBlank
    private String reason;
    
    private Double amount; // Opzionale, se rimborso parziale
}
