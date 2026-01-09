package us.hogu.controller.dto.request;

import lombok.Data;

@Data
public class DistanceRequestDto {
	private double originLat;
    
	private double originLon;
    
	private double destLat;
    
	private double destLon;
}
