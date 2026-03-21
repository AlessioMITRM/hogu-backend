package us.hogu.controller.dto.response;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

import us.hogu.model.enums.BookingStatus;
import us.hogu.model.enums.ServiceType;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class RestaurantBookingResponseDto {
    private Long id;
    private ServiceType serviceType;
    private Long serviceId;
    private String serviceName;
    private String bookingFullName;
    private BookingStatus status;
    private BigDecimal totalAmount;
    private OffsetDateTime creationDate;
    private String bookingCode;

    // Campi specifici ristorante
    private OffsetDateTime reservationTime;
    private Integer numberOfPeople;
    private String specialRequests;
    private String statusReason;
    private java.util.List<String> images;
}
