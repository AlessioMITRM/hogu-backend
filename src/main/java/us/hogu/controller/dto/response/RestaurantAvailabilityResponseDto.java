package us.hogu.controller.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Restaurant availability check response")
public class RestaurantAvailabilityResponseDto {
    @Schema(description = "Restaurant ID", example = "123")
    private Long restaurantId;

    @Schema(description = "Availability status", example = "true")
    private Boolean isAvailable;

    @Schema(description = "Number of available tables", example = "3")
    private Integer availableTables;

    @Schema(description = "Maximum restaurant capacity", example = "50")
    private Integer maxCapacity;

    @Schema(description = "Next available time slots")
    private List<LocalTime> nextAvailableSlots;
}
