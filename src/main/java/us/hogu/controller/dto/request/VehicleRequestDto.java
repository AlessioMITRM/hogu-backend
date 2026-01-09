package us.hogu.controller.dto.request;

import java.util.List;

import lombok.Data;

@Data
public class VehicleRequestDto {
	
	private Long id;

    private String plateNumber; 
    
    private String model;       
    
    private String type;
}
