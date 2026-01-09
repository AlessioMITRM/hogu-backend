package us.hogu.controller.dto.request;

import java.time.LocalDate;

import lombok.Data;

@Data
public class BnbBookingRequestDto {
    private Long bnbServiceId;
    private Long roomId;
    private LocalDate checkInDate;
    private LocalDate checkOutDate;
    private Integer numberOfGuests;
}
