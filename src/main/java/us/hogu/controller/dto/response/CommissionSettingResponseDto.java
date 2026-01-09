package us.hogu.controller.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import us.hogu.model.enums.ServiceType;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;

import javax.validation.constraints.NotNull;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CommissionSettingResponseDto {
	
    private Long id;
    
	private ServiceType serviceType;
    
	private Double commissionRate;
    
	private Double minCommissionAmount;
    
	private OffsetDateTime effectiveFrom;
    
	private OffsetDateTime effectiveTo;
    
	private OffsetDateTime creationDate;

	private OffsetDateTime lastUpdateDate;
}