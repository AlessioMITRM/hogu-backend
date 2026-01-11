package us.hogu.controller.dto.request;

import java.math.BigDecimal;
import java.time.LocalDate;

import lombok.Data;

@Data
public class BnbPriceCalendarRequestDto {
    private LocalDate startDate;
    private LocalDate endDate;
    private BigDecimal pricePerNight;
}
