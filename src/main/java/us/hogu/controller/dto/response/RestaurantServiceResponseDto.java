package us.hogu.controller.dto.response;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
@EqualsAndHashCode(callSuper = true)
public class RestaurantServiceResponseDto extends ServiceDetailResponseDto {
    
	private String menu;
    
    private Integer capacity;
}
