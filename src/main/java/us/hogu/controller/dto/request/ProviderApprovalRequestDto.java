package us.hogu.controller.dto.request;

import javax.validation.constraints.NotBlank;

import io.micrometer.core.lang.NonNull;
import lombok.Data;

@Data
public class ProviderApprovalRequestDto {
	@NonNull
	private Long providerId;
	
	@NonNull
	private Boolean approved;
	
	@NotBlank
	private String notes;
}
