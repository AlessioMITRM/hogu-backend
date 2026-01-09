package us.hogu.controller.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Advanced search parameters for restaurants")
public class RestaurantAdvancedSearchRequestDto {
    @Schema(description = "Location to search restaurants", example = "Milano")
    private String location;

    @Schema(description = "Cuisine type", example = "Italian")
    private String cuisine;

    @Schema(description = "Booking date", example = "2024-02-15")
    private LocalDate date;

    @Schema(description = "Booking time", example = "20:00")
    private LocalTime time;

    @Schema(description = "Minimum price", example = "30.00")
    private BigDecimal minPrice;

    @Schema(description = "Maximum price", example = "100.00")
    private BigDecimal maxPrice;

    @Schema(description = "Number of people", example = "4")
    private Integer numberOfPeople;

    @Schema(description = "Sorting field", example = "rating", allowableValues = {"price", "rating", "distance"})
    private String sortBy;

    @Schema(description = "Sorting direction", example = "desc", allowableValues = {"asc", "desc"})
    private String sortDirection;

    @Schema(description = "Maximum distance in kilometers", example = "10")
    private Double maxDistance;
}
