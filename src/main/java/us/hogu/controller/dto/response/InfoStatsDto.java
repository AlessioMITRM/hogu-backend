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
public class InfoStatsDto {

    private Long clubId;
    private Long totalBookings;
    private Double totalBookingsAmount;
}
