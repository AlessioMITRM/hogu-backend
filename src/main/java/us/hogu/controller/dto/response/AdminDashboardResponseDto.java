package us.hogu.controller.dto.response;

import java.util.List;
import java.util.Map;

import us.hogu.controller.dto.common.PendingActionDto;
import us.hogu.controller.dto.common.RecentActivityDto;
import lombok.Data;

@Data
public class AdminDashboardResponseDto {
    private Map<String, Long> statistics;
    
    private List<RecentActivityDto> recentActivities;
    
    private List<PendingActionDto> pendingActions;
}
