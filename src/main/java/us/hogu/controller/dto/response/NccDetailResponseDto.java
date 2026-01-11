package us.hogu.controller.dto.response;

import java.math.BigDecimal;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NccDetailResponseDto {

	private String name;
	
	private String description;
	
	private BigDecimal basePrice;
	
	private Boolean publicationStatus;
	
	private ServiceLocaleResponseDto locale;
	
	private VehicleEntityResponseDto vehicle;
	
    private List<String> images;
	
}
