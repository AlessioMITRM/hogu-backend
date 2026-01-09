package us.hogu.controller.dto.request;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

import us.hogu.controller.dto.response.CommissionSettingResponseDto;
import us.hogu.model.enums.ServiceType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CommissionSettingRequestDto {
    
	@NotNull
	private ServiceType serviceType;
    
	@NotNull
    private Double commissionRate;
    
	@NotNull
    private Double minCommissionAmount;
    
	@NotNull
    private OffsetDateTime effectiveFrom;
    
	@NotNull
    private OffsetDateTime effectiveTo;
}
