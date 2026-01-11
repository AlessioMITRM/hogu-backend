package us.hogu.controller.dto.response;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

import javax.persistence.Column;

import us.hogu.model.enums.BookingStatus;
import us.hogu.model.enums.ServiceType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LuggageBookingResponseDto {
    private Long id;
    private ServiceType serviceType;
    private Long serviceId;
    private String serviceName;
    private BookingStatus status;
    private BigDecimal totalAmount;
    private OffsetDateTime creationDate;
    
    // Campi specifici bagagli
    private OffsetDateTime pickUpTime;
    private OffsetDateTime dropOffTime;

    @Builder.Default
    private Integer bagsSmall = 0;

    @Builder.Default
    private Integer bagsMedium = 0;

    @Builder.Default
    private Integer bagsLarge = 0;
    
    private String specialRequests;
}