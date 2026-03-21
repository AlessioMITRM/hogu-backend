package us.hogu.controller.dto.response;

import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdminDashboardKpiResponseDto {
    private Long totalUsers;
    private Long pendingProviders;
    private BigDecimal totalRevenue;
}
