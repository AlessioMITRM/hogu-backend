package us.hogu.controller.dto.response;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import us.hogu.model.enums.PaymentStatus;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PayoutResponseDto {
    private Long id;
    
    private Long providerId;
    
    private BigDecimal amount;
    
    private PaymentStatus status;
    
    private OffsetDateTime paymentDate;
}
