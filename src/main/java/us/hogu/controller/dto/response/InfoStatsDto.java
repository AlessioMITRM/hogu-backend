package us.hogu.controller.dto.response;

import java.math.BigDecimal;

import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
public class InfoStatsDto {
    private Long serviceId;
    private String name;
    private String description;
    private Long totalBookings;
    private BigDecimal totalBookingsAmount;
    private BigDecimal totalCommissionsAmount;

    public InfoStatsDto(Long serviceId, String name, String description, Long totalBookings,
            BigDecimal totalBookingsAmount) {
        this.serviceId = serviceId;
        this.name = name;
        this.description = description;
        this.totalBookings = totalBookings;
        this.totalBookingsAmount = totalBookingsAmount;
    }

    public InfoStatsDto(Long serviceId, String name, String description, Long totalBookings,
            BigDecimal totalBookingsAmount, BigDecimal totalCommissionsAmount) {
        this.serviceId = serviceId;
        this.name = name;
        this.description = description;
        this.totalBookings = totalBookings;
        this.totalBookingsAmount = totalBookingsAmount;
        this.totalCommissionsAmount = totalCommissionsAmount;
    }
}
