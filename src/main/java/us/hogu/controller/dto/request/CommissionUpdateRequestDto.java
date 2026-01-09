package us.hogu.controller.dto.request;

import javax.validation.constraints.NotBlank;

import io.micrometer.core.lang.NonNull;
import lombok.Data;
import us.hogu.model.enums.ServiceType;

@Data
public class CommissionUpdateRequestDto {
	@NotBlank
	private ServiceType serviceType;
	
	@NonNull
	private Double commissionRate;
	
	@NonNull
	private Double minCommissionAmount;
}
