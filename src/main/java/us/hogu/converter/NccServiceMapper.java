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

import us.hogu.controller.dto.response.NccDetailResponseDto;
import us.hogu.controller.dto.response.VehicleEntityResponseDto;
import us.hogu.controller.dto.response.ServiceLocaleResponseDto;
import us.hogu.model.VehicleEntity;
import us.hogu.model.ServiceLocale;

@RequiredArgsConstructor
@Component
public class NccServiceMapper {
    private final ServiceLocaleMapper serviceLocaleMapper;
    private final VehicleMapper vehicleMapper;
    
    
    public NccServiceEntity toEntity(NccServiceRequestDto dto) {
    	
        return NccServiceEntity.builder()
            .name(dto.getName())
            .description(dto.getDescription())
            .vehiclesAvailable(dto.getVehicle() != null ? List.of(vehicleMapper.toEntity(dto.getVehicle())) : List.of())
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
    
    public NccDetailResponseDto toNccDetailDto(NccServiceEntity entity) {
    	ServiceLocaleResponseDto localeDto = null;
    	if (entity.getLocales() != null && !entity.getLocales().isEmpty()) {
    		// Assuming we want the first locale or filter by language if needed
    		// But NccDetailResponseDto has a single locale
    		ServiceLocale locale = entity.getLocales().get(0);
    		localeDto = ServiceLocaleResponseDto.builder()
    				.serviceId(locale.getId())
    				.serviceType(locale.getServiceType())
    				.language(locale.getLanguage())
    				.country(locale.getCountry())
    				.state(locale.getState())
    				.city(locale.getCity())
    				.address(locale.getAddress())
    				.build();
    	}
    	
    	VehicleEntityResponseDto vehicleDto = null;
    	if (entity.getVehiclesAvailable() != null && !entity.getVehiclesAvailable().isEmpty()) {
    		VehicleEntity vehicle = entity.getVehiclesAvailable().get(0);
    		vehicleDto = VehicleEntityResponseDto.builder()
    				.id(vehicle.getId())
    				.numberOfSeats(vehicle.getNumberOfSeats())
    				.plateNumber(vehicle.getPlateNumber())
    				.model(vehicle.getModel())
    				.type(vehicle.getType())
    				.build();
    	}
    	
        return NccDetailResponseDto.builder()
            .name(entity.getName())
            .description(entity.getDescription())
            .basePrice(entity.getBasePrice())
            .publicationStatus(entity.getPublicationStatus())
            .locale(localeDto)
            .vehicle(vehicleDto)
            .images(entity.getImages())
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
        if (dto.getVehicle() != null) entity.setVehiclesAvailable(List.of(vehicleMapper.toEntity(dto.getVehicle())));
        if (dto.getBasePrice() != null) entity.setBasePrice(dto.getBasePrice());
        
        if (dto.getLocales() != null) {
        	entity.setLocales(serviceLocaleMapper.mapRequestToEntity(dto.getLocales()));
        }
    }
    
}
