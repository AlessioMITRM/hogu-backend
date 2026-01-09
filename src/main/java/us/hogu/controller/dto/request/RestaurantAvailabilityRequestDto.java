package us.hogu.controller.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Restaurant availability check request")
public class RestaurantAvailabilityRequestDto {
    @Schema(description = "Date of desired booking", example = "2024-02-15")
    private LocalDate date;

    @Schema(description = "Time of desired booking", example = "20:00")
    private LocalTime time;

    @Schema(description = "Number of people", example = "4")
    private Integer numberOfPeople;
}
