package us.hogu.controller.dto.response;

import java.time.LocalTime;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class OpeningHourResponseDto {
    private Long id;
    private Integer dayOfWeek;
    private LocalTime openingTime;
    private LocalTime closingTime;
    private Boolean closed;
}