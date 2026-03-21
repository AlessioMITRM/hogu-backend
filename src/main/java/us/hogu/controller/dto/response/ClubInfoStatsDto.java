package us.hogu.controller.dto.response;

import java.math.BigDecimal;

import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
public class ClubInfoStatsDto {

    private Long clubId;
    private String name;
    private String description;
    private Long totalBookings;
    private BigDecimal totalBookingsAmount;

    public ClubInfoStatsDto(Long clubId, String name, String description, Long totalBookings,
            BigDecimal totalBookingsAmount) {
        this.clubId = clubId;
        this.name = name;
        this.description = description;
        this.totalBookings = totalBookings;
        this.totalBookingsAmount = totalBookingsAmount;
    }
}
