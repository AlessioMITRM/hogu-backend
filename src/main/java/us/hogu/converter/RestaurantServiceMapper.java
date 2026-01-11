package us.hogu.converter;

import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import us.hogu.common.util.ImageUtils;
import us.hogu.controller.dto.request.RestaurantServiceRequestDto;
import us.hogu.controller.dto.response.ProviderSummaryResponseDto;
import us.hogu.controller.dto.response.RestaurantManagementResponseDto;
import us.hogu.controller.dto.response.RestaurantServiceDetailResponseDto;
import us.hogu.controller.dto.response.RestaurantServiceResponseDto;
import us.hogu.controller.dto.response.ServiceSummaryResponseDto;
import us.hogu.model.RestaurantServiceEntity;
import us.hogu.model.User;
import us.hogu.model.enums.ServiceType;

@RequiredArgsConstructor
@Component
public class RestaurantServiceMapper {
    private final ServiceLocaleMapper serviceLocaleMapper;
	
    public RestaurantServiceEntity toEntity(RestaurantServiceRequestDto dto) {
       	
        return RestaurantServiceEntity.builder()
            .name(dto.getName())
            .description(dto.getDescription())
            .locales(serviceLocaleMapper.mapRequestToEntity(dto.getLocales()))
            .menu(dto.getMenu())
            .capacity(dto.getCapacity())
            .basePrice(dto.getBasePrice())
            .images(dto.getImages())
            .publicationStatus(dto.getPublicationStatus())
            .creationDate(OffsetDateTime.now())
            .build();
    }
    
    public RestaurantServiceResponseDto toDetailDto(RestaurantServiceEntity entity) {
    	return RestaurantServiceResponseDto.builder()
    	        .id(entity.getId())
    	        .name(entity.getName())
    	        .description(entity.getDescription())
    	        .serviceLocale( serviceLocaleMapper.mapEntityToReponse(entity.getLocales()))
    	        .menu(entity.getMenu())
    	        .capacity(entity.getCapacity())
    	        .basePrice(entity.getBasePrice())
    	        .images(entity.getImages())
    	        .serviceType(ServiceType.RESTAURANT)
    	    	.provider(ProviderSummaryResponseDto.builder().id(entity.getUser().getId())
                .name(entity.getUser().getName()).build())
    	    	.basePrice(entity.getBasePrice())
    	        .build();
    }
    
    public RestaurantServiceResponseDto toDetailDto(RestaurantServiceEntity entity, User provider) {
    	
    	return RestaurantServiceResponseDto.builder()
    	        .id(entity.getId())
    	        .name(entity.getName())
    	        .description(entity.getDescription())
    	        .serviceLocale(serviceLocaleMapper.mapEntityToReponse(entity.getLocales()))
    	        .menu(entity.getMenu())
    	        .capacity(entity.getCapacity())
    	        .basePrice(entity.getBasePrice())
    	        .images(entity.getImages())
    	        .serviceType(ServiceType.RESTAURANT)
    	    	.provider(ProviderSummaryResponseDto.builder().id(provider.getId())
        				.name(provider.getName()).build())
    	        .build();
    }

    public RestaurantServiceDetailResponseDto toProviderDetailDto(RestaurantServiceEntity entity) {
        return RestaurantServiceDetailResponseDto.builder()
                .id(entity.getId())
                .name(entity.getName())
                .description(entity.getDescription())
                .locales(serviceLocaleMapper.mapEntityToReponse(entity.getLocales()))
                .menu(entity.getMenu())
                .capacity(entity.getCapacity())
                .basePrice(entity.getBasePrice())
                .images(entity.getImages())
                .publicationStatus(entity.getPublicationStatus())
                .creationDate(entity.getCreationDate())
                .providerId(entity.getUser().getId())
                .build();
    }

    public void updateEntityFromDto(RestaurantServiceRequestDto dto, RestaurantServiceEntity entity) {
        if (dto.getName() != null) entity.setName(dto.getName());
        if (dto.getDescription() != null) entity.setDescription(dto.getDescription());
        if (dto.getLocales() != null) entity.setLocales(null);
        if (dto.getMenu() != null) entity.setMenu(dto.getMenu());
        if (dto.getCapacity() != null) entity.setCapacity(dto.getCapacity());
        if (dto.getBasePrice() != null) entity.setBasePrice(dto.getBasePrice());
        if (dto.getImages() != null && !dto.getImages().isEmpty()) {
            entity.setImages(dto.getImages());
        }
    }
    
    public ServiceSummaryResponseDto toSummaryDto(RestaurantServiceEntity entity) {
        return ServiceSummaryResponseDto.builder()
            .id(entity.getId())
            .name(entity.getName())
            .description(truncateDescription(entity.getDescription(), 100))
            .basePrice(entity.getBasePrice())
            .images(entity.getImages())
            .locales(serviceLocaleMapper.mapEntityToReponse(entity.getLocales()))
            .serviceType(ServiceType.RESTAURANT)
            .build();
    }
    
    public RestaurantManagementResponseDto toManagementDto(RestaurantServiceEntity entity) {
        if (entity == null) {
            return null;
        }

        return RestaurantManagementResponseDto.builder()
                .id(entity.getId())
                .name(entity.getName())
                .publicationStatus(entity.getPublicationStatus())
                .creationDate(entity.getCreationDate())
                .userId(entity.getUser().getId())
                .build();
    }
    
    public List<RestaurantManagementResponseDto> toManagementDtoList(List<RestaurantServiceEntity> entities) {
        if (entities == null) {
            return List.of();
        }

        return entities.stream()
                .map(this::toManagementDto)
                .collect(Collectors.toList());
    }
    
    // Metodi helper
    private String truncateDescription(String description, int maxLength) {
        if (description == null) return "";
        return description.length() > maxLength ? 
            description.substring(0, maxLength) + "..." : description;
    }
    
    private List<String> parseImagePaths(String imagesJson) {
        if (imagesJson == null || imagesJson.trim().isEmpty()) {
            return Collections.emptyList();
        }
        try {
            return Arrays.asList(imagesJson.split(","));
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }

}
