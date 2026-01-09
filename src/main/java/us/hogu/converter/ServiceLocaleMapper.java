package us.hogu.converter;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import us.hogu.controller.dto.request.ServiceLocaleRequestDto;
import us.hogu.controller.dto.response.ServiceLocaleResponseDto;
import us.hogu.model.ServiceLocale;
import us.hogu.model.enums.LanguageType;
import us.hogu.model.enums.ServiceType;

@Component
public class ServiceLocaleMapper {
	
	public List<ServiceLocaleResponseDto> mapEntityToReponse(List<ServiceLocale> locales) {
        if (locales == null) {
            return List.of();
        }
        return locales.stream()
                .map(locale -> ServiceLocaleResponseDto.builder()
                        .language(locale.getLanguage())
                        .country(locale.getCountry())
                        .state(locale.getState())
                        .city(locale.getCity())
                        .address(locale.getAddress())
                        .build())
                .collect(Collectors.toList());
    }

    public List<ServiceLocale> mapRequestToEntity(List<ServiceLocaleRequestDto> requestDtos) {
        if (requestDtos == null) {
            return new ArrayList<>();
        }

        return requestDtos.stream()
                .map((ServiceLocaleRequestDto locale) -> ServiceLocale.builder()
                        .serviceType(ServiceType.NCC)
                        .language(LanguageType.fromValue(locale.getLanguage()).getValue())
                        .country(locale.getCountry())
                        .state(locale.getState())
                        .city(locale.getCity())
                        .address(locale.getAddress())
                        .build())
                .collect(Collectors.toList());
    }
}
