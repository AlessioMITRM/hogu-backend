package us.hogu.controller.dto.request;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

import lombok.Data;

@Data
public class EventClubServiceRequestDto {

    private Long clubServiceId;

    private String name;

    private String description;

    private List<ServiceLocaleRequestDto> serviceLocale;

    private OffsetDateTime startTime;

    private OffsetDateTime endTime;

    private BigDecimal price;

    private Long maxCapacity;

    private String theme;

    private Boolean isActive;

    private String dressCode;
    
    private String djName;

    private Integer genderPercentage;

    private List<EventPricingConfigurationRequestDto> pricingConfigurations;
}
