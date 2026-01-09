package us.hogu.controller.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class DistanceResponseDto {
	private double distanceKm;
    
	private double durationMinutes;
}
