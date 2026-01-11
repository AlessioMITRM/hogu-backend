package us.hogu.controller.dto.response;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

import lombok.Data;
import us.hogu.model.enums.PaymentStatus;

@Data
public class RefundResponseDto {
    private Long id;
    
    private Long paymentId;
    
    private BigDecimal amount;
    
    private PaymentStatus status;
    
    private OffsetDateTime processedAt;
}
