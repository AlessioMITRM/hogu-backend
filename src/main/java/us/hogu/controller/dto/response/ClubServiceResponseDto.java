package us.hogu.controller.dto.response;

import java.math.BigDecimal;
import java.util.List;

import us.hogu.controller.dto.common.EventDto;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
@EqualsAndHashCode(callSuper = true)
public class ClubServiceResponseDto extends ServiceDetailResponseDto {
	private List<EventDto> events;
	
    private Long maxCapacity;    
    
    private Boolean hasEvents;     
}
