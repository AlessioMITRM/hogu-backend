package us.hogu.controller.dto.common;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EventDto {
    private Long id;
    
    private String name;
    
    private String description;
    
    private OffsetDateTime startTime;
    
    private OffsetDateTime endTime;
    
    private BigDecimal price;
    
    private Long maxCapacity;
    
    private String djName;
    
    private String theme;
}
