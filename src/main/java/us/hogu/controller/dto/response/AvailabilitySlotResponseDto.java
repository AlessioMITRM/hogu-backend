package us.hogu.controller.dto.response;

import java.time.LocalDate;
import java.time.OffsetDateTime;

import lombok.Data;
import us.hogu.model.enums.ServiceType;

@Data
public class AvailabilitySlotResponseDto {
    private Long id;
    
    private ServiceType serviceType;
    
    private Long serviceId;
    
    private LocalDate date;
    
    private OffsetDateTime startTime;
    
    private OffsetDateTime endTime;
    
    private Integer maxCapacity;
    
    private Integer availableSlots;
}
