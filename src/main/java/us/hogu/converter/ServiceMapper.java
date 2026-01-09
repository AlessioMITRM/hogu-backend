package us.hogu.converter;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import us.hogu.controller.dto.response.ServiceSummaryResponseDto;
import us.hogu.model.LuggageServiceEntity;
import us.hogu.model.NccServiceEntity;
import us.hogu.model.RestaurantServiceEntity;
import us.hogu.model.enums.ServiceType;
import us.hogu.repository.jpa.EventClubServiceRepository;

@RequiredArgsConstructor
@Component
public class ServiceMapper {
    private final ServiceLocaleMapper serviceLocaleMapper;

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
    
    public ServiceSummaryResponseDto toSummaryDto(NccServiceEntity entity) {
        return ServiceSummaryResponseDto.builder()
            .id(entity.getId())
            .name(entity.getName())
            .description(truncateDescription(entity.getDescription(), 100))
            .basePrice(entity.getBasePrice())
            .images(entity.getImages())
            .serviceType(ServiceType.NCC)
            .build();
    }
    
    public ServiceSummaryResponseDto toSummaryDto(LuggageServiceEntity entity) {
        return ServiceSummaryResponseDto.builder()
            .id(entity.getId())
            .name(entity.getName())
            .description(truncateDescription(entity.getDescription(), 100))
            .basePrice(entity.getBasePrice())
            .images(entity.getImages())
            .locales(serviceLocaleMapper.mapEntityToReponse(entity.getLocales()))
            .serviceType(ServiceType.LUGGAGE)
            .build();
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
