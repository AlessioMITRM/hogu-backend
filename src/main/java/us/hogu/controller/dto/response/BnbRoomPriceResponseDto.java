package us.hogu.controller.dto.response;

import java.time.LocalDate;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class BnbRoomPriceResponseDto {
    private Long id;
    private LocalDate startDate;
    private LocalDate endDate;
    private Double pricePerNight;
    private Long roomId;
}