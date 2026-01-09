package us.hogu.controller.dto.request;

import javax.validation.constraints.NotBlank;

import io.micrometer.core.lang.NonNull;
import lombok.Data;

@Data
public class PayoutRequestDto {
	@NonNull
    private Long providerId;
    
	@NonNull
    private Double amount;
}
