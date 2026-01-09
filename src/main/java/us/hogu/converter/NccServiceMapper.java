package us.hogu.converter;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import us.hogu.common.util.ImageUtils;
import us.hogu.controller.dto.request.NccServiceRequestDto;
import us.hogu.controller.dto.response.NccManagementResponseDto;
import us.hogu.controller.dto.response.NccServiceResponseDto;
import us.hogu.controller.dto.response.ProviderSummaryResponseDto;
import us.hogu.controller.dto.response.ServiceDetailResponseDto;
import us.hogu.model.NccServiceEntity;
import us.hogu.model.User;
import us.hogu.model.enums.ServiceType;
import us.hogu.repository.jpa.EventClubServiceRepository;

@RequiredArgsConstructor
@Component
public class NccServiceMapper {
    private final ServiceLocaleMapper serviceLocaleMapper;
    private final VehicleMapper vehicleMapper;
    
    
    public NccServiceEntity toEntity(NccServiceRequestDto dto) {
    	
        return NccServiceEntity.builder()
            .name(dto.getName())
            .description(dto.getDescription())
            .vehiclesAvailable(vehicleMapper.toEntityList(dto.getVehiclesAvailable()))
            .basePrice(dto.getBasePrice())
            .locales(serviceLocaleMapper.mapRequestToEntity(dto.getLocales()))
            .publicationStatus(dto.getPublicationStatus())
            .build();
    }
    
    // Per Dettaglio da entity (FRONTEND)
    public NccServiceResponseDto toDetailDto(NccServiceEntity entity, User provider) {
        return NccServiceResponseDto.builder()
            .id(entity.getId())
            .name(entity.getName())
            .description(entity.getDescription())
            .vehiclesAvailable(vehicleMapper.toDtoList(entity.getVehiclesAvailable()))
            .basePrice(entity.getBasePrice())
            .serviceLocale(serviceLocaleMapper.mapEntityToReponse(entity.getLocales()))
            .images(entity.getImages())
            .provider(ProviderSummaryResponseDto.builder()
                .id(provider.getId())
                .name(provider.getName())
                .build())
            .serviceType(ServiceType.NCC)
            .build();
    }
    
    public NccServiceResponseDto toDetailDto(NccServiceEntity entity) {
        return NccServiceResponseDto.builder()
                .id(entity.getId())
                .name(entity.getName())
                .description(entity.getDescription())
                .locales(serviceLocaleMapper.mapEntityToReponse(entity.getLocales()))
                .vehiclesAvailable(vehicleMapper.toDtoList(entity.getVehiclesAvailable()))
                .basePrice(entity.getBasePrice())
                .images(entity.getImages())
                .serviceType(ServiceType.NCC)
                .provider(ProviderSummaryResponseDto.builder()
                        .id(entity.getUser().getId())
                        .name(entity.getUser().getName())
                        .build())
                .build();
    }
    
    public NccManagementResponseDto toManagementDto(NccServiceEntity entity) {
        if (entity == null) {
            return null;
        }

        return NccManagementResponseDto.builder()
                .id(entity.getId())
                .name(entity.getName())
                .publicationStatus(entity.getPublicationStatus())
                .creationDate(entity.getCreationDate())
                .build();
    }
    
    public List<NccManagementResponseDto> toManagementDtoList(List<NccServiceEntity> entities) {
        if (entities == null) {
            return List.of(); // lista vuota invece di null
        }

        return entities.stream()
                .map(this::toManagementDto)
                .collect(Collectors.toList());
    }
    
    public void updateEntityFromDto(NccServiceRequestDto dto, NccServiceEntity entity) {
        if (dto.getName() != null) entity.setName(dto.getName());
        if (dto.getDescription() != null) entity.setDescription(dto.getDescription());
        if (dto.getVehiclesAvailable() != null) entity.setVehiclesAvailable(vehicleMapper.toEntityList(dto.getVehiclesAvailable()));
        if (dto.getBasePrice() != null) entity.setBasePrice(dto.getBasePrice());
        if (dto.getLocales() != null) entity.setLocales(serviceLocaleMapper.mapRequestToEntity(dto.getLocales()));
    }
    
}
