package us.hogu.controller.dto.response;

import java.time.OffsetDateTime;

import lombok.Data;

@Data
public class AppliedCommissionResponseDto {
    private Long id;
    
    private Long bookingId;
    
    private Double commissionAmount;
    
    private Double commissionRateApplied;
    
    private OffsetDateTime calculatedAt;
}
