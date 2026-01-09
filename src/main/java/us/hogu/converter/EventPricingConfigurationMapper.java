package us.hogu.converter;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import us.hogu.controller.dto.request.EventPricingConfigurationRequestDto;
import us.hogu.controller.dto.response.EventPricingConfigurationResponseDto;
import us.hogu.model.EventPricingConfiguration;
import us.hogu.model.EventClubServiceEntity;

@Component
public class EventPricingConfigurationMapper {
    
    public EventPricingConfiguration toEntity(EventPricingConfigurationRequestDto dto, EventClubServiceEntity event) {
        return EventPricingConfiguration.builder()
            .eventClubService(event)
            .pricingType(dto.getPricingType())
            .description(dto.getDescription())
            .price(dto.getPrice())
            .capacity(dto.getCapacity())
            .isActive(dto.getIsActive() != null ? dto.getIsActive() : true)
            .build();
    }
    
    public EventPricingConfigurationResponseDto toDto(EventPricingConfiguration entity) {
        return EventPricingConfigurationResponseDto.builder()
            .id(entity.getId())
            .pricingType(entity.getPricingType())
            .description(entity.getDescription())
            .price(entity.getPrice())
            .capacity(entity.getCapacity())
            .isActive(entity.getIsActive())
            .build();
    }
    
    public List<EventPricingConfiguration> toEntityList(List<EventPricingConfigurationRequestDto> dtos, EventClubServiceEntity event) {
        if (dtos == null) {
            return List.of();
        }
        return dtos.stream()
            .map(dto -> toEntity(dto, event))
            .collect(Collectors.toList());
    }
    
    public List<EventPricingConfigurationResponseDto> toDtoList(List<EventPricingConfiguration> entities) {
        if (entities == null) {
            return List.of();
        }
        return entities.stream()
            .map(this::toDto)
            .collect(Collectors.toList());
    }
}