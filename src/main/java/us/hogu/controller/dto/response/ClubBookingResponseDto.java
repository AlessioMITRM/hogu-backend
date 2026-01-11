package us.hogu.controller.dto.response;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

import us.hogu.model.enums.BookingStatus;
import us.hogu.model.enums.ServiceType;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ClubBookingResponseDto {
    private Long id;
    private Long eventId;
    private String bookingFullName;
    private String eventName;
    private BookingStatus status;
    private BigDecimal totalAmount;
    private OffsetDateTime creationDate;    
    private OffsetDateTime reservationTime;
    private Integer numberOfPeople;
    private String specialRequests;
    private Boolean table;
}
