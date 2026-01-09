package us.hogu.controller.dto.request;

import java.time.LocalTime;

import javax.validation.constraints.NotNull;

import lombok.Data;

@Data
public class OpeningHourRequestDto {

    @NotNull
    private Integer dayOfWeek; // 1 = Monday ... 7 = Sunday

    @NotNull
    private LocalTime openingTime;

    @NotNull
    private LocalTime closingTime;

    @NotNull
    private Boolean closed; // true = chiuso per tutto il giorno
}

