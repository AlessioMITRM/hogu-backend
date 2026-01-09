package us.hogu.controller.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class VehicleResponseDto {
    
	private Long id;

    private String plateNumber; 
    
    private String model;       
    
    private String type;
}
