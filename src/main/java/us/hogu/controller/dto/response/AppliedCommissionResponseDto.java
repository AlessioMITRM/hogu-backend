package us.hogu.controller.dto.response;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

import lombok.Data;

@Data
public class AppliedCommissionResponseDto {
    private Long id;
    
    private Long bookingId;
    
    private BigDecimal commissionAmount;
    
    private BigDecimal commissionRateApplied;
    
    private OffsetDateTime calculatedAt;
}
