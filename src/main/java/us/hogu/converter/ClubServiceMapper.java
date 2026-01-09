package us.hogu.converter;

import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import us.hogu.common.util.ImageUtils;
import us.hogu.controller.dto.common.EventDto;
import us.hogu.controller.dto.request.ClubServiceRequestDto;
import us.hogu.controller.dto.request.EventCreateRequestDto;
import us.hogu.controller.dto.response.ClubServiceResponseDto;
import us.hogu.controller.dto.response.ProviderSummaryResponseDto;
import us.hogu.controller.dto.response.ServiceDetailResponseDto;
import us.hogu.controller.dto.response.ServiceSummaryResponseDto;
import us.hogu.model.ClubServiceEntity;
import us.hogu.model.EventClubServiceEntity;
import us.hogu.model.User;
import us.hogu.model.enums.ServiceType;
import us.hogu.repository.jpa.EventClubServiceRepository;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Component
public class ClubServiceMapper {
    private final EventClubServiceRepository eventClubServiceRepository;
    private final ServiceLocaleMapper serviceLocaleMapper;
    
    public ClubServiceEntity toEntity(ClubServiceRequestDto dto, User provider) {
        return ClubServiceEntity.builder()
            .user(provider)
            .name(dto.getName())
            .description(dto.getDescription())
            .locales(serviceLocaleMapper.mapRequestToEntity(dto.getLocales()))
            .maxCapacity(dto.getMaxCapacity())
            .basePrice(dto.getBasePrice())
            .publicationStatus(dto.getPublicationStatus())
            .build();
    }
    
    public ClubServiceResponseDto toDetailDto(ClubServiceEntity entity) {
        // Carica gli eventi attivi
        List<EventClubServiceEntity> events = eventClubServiceRepository
            .findByclubServiceIdAndIsActiveTrue(entity.getId());
        
        return ClubServiceResponseDto.builder()
            .id(entity.getId())
            .name(entity.getName())
            .description(entity.getDescription())
            .serviceLocale(serviceLocaleMapper.mapEntityToReponse(entity.getLocales()))
            .maxCapacity(entity.getMaxCapacity())
            .events(mapEventsToDto(events))
            .basePrice(entity.getBasePrice())
            .images(entity.getImages())
            .provider(ProviderSummaryResponseDto.builder()
                    .id(entity.getUser().getId())
                    .name(entity.getUser().getName())
                    .build())
            .serviceType(ServiceType.CLUB)
            .publicationStatus(entity.getPublicationStatus())
            .build();
    }
    
  /*  public ClubServiceResponseDto toSummaryDto(ClubServiceEntity entity) {
        return ClubServiceResponseDto.builder()
            .id(entity.getId())
            .name(entity.getName())
            .description(entity.getDescription())
            .address(entity.getAddress())
            .capacity(entity.getCapacity())
            .basePrice(entity.getBasePrice())
            .images(entity.getImages())
            .serviceType(ServiceType.CLUB)
            .hasEvents(hasActiveEvents(entity.getId()))
            .build();
    }*/
    
    public ServiceSummaryResponseDto toSummaryDto(ClubServiceEntity entity) {
        return ServiceSummaryResponseDto.builder()
            .id(entity.getId())
            .name(entity.getName())
            .description(entity.getDescription())
            .locales(serviceLocaleMapper.mapEntityToReponse(entity.getLocales()))
            .basePrice(entity.getBasePrice())
            .serviceType(ServiceType.CLUB)
            .images(entity.getImages())
            .build();
    }
    
    private List<EventDto> mapEventsToDto(List<EventClubServiceEntity> events) {
        return events.stream()
            .map(this::toEventDto)
            .collect(Collectors.toList());
    }
    
    private EventDto toEventDto(EventClubServiceEntity event) {
        return EventDto.builder()
            .id(event.getId())
            .name(event.getName())
            .description(event.getDescription())
            .startTime(event.getStartTime())
            .endTime(event.getEndTime())
            .price(event.getPrice())
            .maxCapacity(event.getMaxCapacity())
            .djName(event.getDjName())
            .theme(event.getTheme())
            .build();
    }
    
    private boolean hasActiveEvents(Long clubServiceId) {
        List<EventClubServiceEntity> events = eventClubServiceRepository
            .findByclubServiceIdAndIsActiveTrue(clubServiceId);
        return !events.isEmpty();
    }
}
