package us.hogu.controller.dto.request;

import java.time.LocalDate;
import java.time.OffsetDateTime;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

import lombok.Data;
import us.hogu.model.enums.ServiceType;

@Data
public class AvailabilitySlotRequestDto {
	
	@NotNull
    private ServiceType serviceType;
	
	@NotNull
	private Long serviceId;
	
	@NotNull
	private LocalDate date;
	
	@NotNull
	private OffsetDateTime startTime;
	
	@NotNull
	private OffsetDateTime endTime;
	
	@NotNull
	private Integer maxCapacity;
	
	@NotNull
	private Integer availableSlots;
}
