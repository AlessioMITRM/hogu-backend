package us.hogu.controller.dto.response;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EventClubServiceResponseDto {

    private Long id;
    
    private Long clubServiceId;

    private String name;

    private String description;

    private List<ServiceLocaleResponseDto> serviceLocale;

    private OffsetDateTime startTime;

    private OffsetDateTime endTime;

    private String theme;

    private Boolean available;

    private BigDecimal price;

    private Long maxCapacity;

    private Long occupiedCapacity;

    private String dressCode;

    private Integer genderPercentage;
    
    private String djName;

    private List<String> images;

    private List<EventPricingConfigurationResponseDto> pricingConfigurations;
}
