package us.hogu.controller.dto.response;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import us.hogu.model.enums.BookingStatus;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminBookingResponseDto {
    private Long id;
    private String bookingCode;
    private OffsetDateTime creationDate;
    private String customerName;
    private String customerEmail;
    private BigDecimal totalAmount;
    private BookingStatus status;
    private String serviceType;
    private String providerName;
    private String providerEmail;
    private String statusReason;
}
