package us.hogu.controller.dto.request;

import java.time.LocalDate;

import lombok.Data;

@Data
public class BnbRoomPriceRequestDto {
    private LocalDate startDate;
    private LocalDate endDate;
    private Double pricePerNight;
}
