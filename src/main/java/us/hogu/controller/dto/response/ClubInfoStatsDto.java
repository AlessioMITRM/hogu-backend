package us.hogu.controller.dto.response;

import java.math.BigDecimal;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClubInfoStatsDto {

    private Long clubId;
    private Long totalBookings;
    private BigDecimal totalBookingsAmount;
}
