package us.hogu.controller.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;

import lombok.Builder;
import lombok.Data;
import us.hogu.model.enums.BookingStatus;

import java.time.LocalDate;
import java.time.OffsetDateTime;

import lombok.Builder;
import lombok.Data;
import us.hogu.model.enums.BookingStatus;

@Data
@Builder
public class BnbBookingResponseDto {
    private Long id;
    private Long serviceId;
    private String serviceName;
    private Long roomId;
    private String roomName;
    private LocalDate checkInDate;
    private LocalDate checkOutDate;
    private Integer numberOfGuests;
    private BookingStatus status;
    private BigDecimal totalAmount;
    private OffsetDateTime creationDate;
}
