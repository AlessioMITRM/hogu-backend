package us.hogu.converter;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import us.hogu.controller.dto.request.LuggageServiceRequestDto;
import us.hogu.controller.dto.request.LuggageSizePriceRequestDto;
import us.hogu.controller.dto.request.OpeningHourRequestDto;
import us.hogu.controller.dto.response.LuggageServiceAdminResponseDto;
import us.hogu.controller.dto.response.LuggageServiceProviderResponseDto;
import us.hogu.controller.dto.response.LuggageServiceResponseDto;
import us.hogu.controller.dto.response.LuggageSizePriceResponseDto;
import us.hogu.controller.dto.response.OpeningHourResponseDto;
import us.hogu.controller.dto.response.ProviderSummaryResponseDto;
import us.hogu.model.LuggageServiceEntity;
import us.hogu.model.LuggageSizePrice;
import us.hogu.model.OpeningHour;
import us.hogu.model.User;
import us.hogu.model.enums.ServiceType;

@RequiredArgsConstructor
@Component
public class LuggageServiceMapper {

    private final ServiceLocaleMapper serviceLocaleMapper;

    // -------------------- REQUEST → ENTITY --------------------
    public LuggageServiceEntity toEntity(LuggageServiceRequestDto dto) {
        LuggageServiceEntity entity = LuggageServiceEntity.builder()
            .name(dto.getName())
            .description(dto.getDescription())
            .locales(serviceLocaleMapper.mapRequestToEntity(dto.getLocales()))
            .capacity(dto.getCapacity())
            .basePrice(dto.getBasePrice())
            .publicationStatus(dto.getPublicationStatus())
            .creationDate(OffsetDateTime.now())
            .build();

        if (dto.getSizePrices() != null && !dto.getSizePrices().isEmpty()) {
            entity.setSizePrices(dto.getSizePrices().stream()
                .map(this::toSizePriceEntity)
                .collect(Collectors.toList()));

            entity.getSizePrices().forEach(sp -> sp.setLuggageService(entity));
        }
        
        // Opening hours
        if (dto.getOpeningHours() != null && !dto.getOpeningHours().isEmpty()) {
            List<OpeningHour> hours = dto.getOpeningHours().stream()
                .map(h -> toOpeningHourEntity(h, entity))
                .collect(Collectors.toList());
            entity.setOpeningHours(hours);
        }

        return entity;
    }


    private LuggageSizePrice toSizePriceEntity(LuggageSizePriceRequestDto dto) {
        return LuggageSizePrice.builder()
            .sizeLabel(dto.getSizeLabel())
            .pricePerHour(dto.getPricePerHour())
            .pricePerDay(dto.getPricePerDay())
            .description(dto.getDescription())
            .build();
    }

    // -------------------- ENTITY → RESPONSE --------------------
    public LuggageServiceResponseDto toDetailDto(LuggageServiceEntity entity) {
        return LuggageServiceResponseDto.builder()
            .id(entity.getId())
            .name(entity.getName())
            .description(entity.getDescription())
            .locales(serviceLocaleMapper.mapEntityToReponse(entity.getLocales()))
            .capacity(entity.getCapacity())
            .basePrice(entity.getBasePrice())
            .images(entity.getImages())
            .publicationStatus(entity.getPublicationStatus())
            .sizePrices(entity.getSizePrices() != null
                ? entity.getSizePrices().stream()
                    .map(this::toSizePriceResponseDto)
                    .collect(Collectors.toList())
                : null)
            .openingHours(entity.getOpeningHours() != null
            ? entity.getOpeningHours().stream()
                .map(this::toOpeningHourResponseDto)
                .collect(Collectors.toList())
            : null)
            .serviceType(ServiceType.LUGGAGE)
            .build();
    }

    private LuggageSizePriceResponseDto toSizePriceResponseDto(LuggageSizePrice entity) {
        return LuggageSizePriceResponseDto.builder()
            .id(entity.getId())
            .sizeLabel(entity.getSizeLabel())
            .pricePerHour(entity.getPricePerHour())
            .pricePerDay(entity.getPricePerDay())
            .description(entity.getDescription())
            .build();
    }

    // -------------------- ADMIN / PROVIDER DTOs --------------------
    public LuggageServiceAdminResponseDto toAdminDto(LuggageServiceEntity entity) {
        return LuggageServiceAdminResponseDto.builder()
            .id(entity.getId())
            .name(entity.getName())
            .publicationStatus(entity.getPublicationStatus())
            .creationDate(entity.getCreationDate())
            .capacity(entity.getCapacity())
            .providerName(entity.getUser() != null ? entity.getUser().getName() : null)
            .build();
    }

    public LuggageServiceProviderResponseDto toProviderDto(LuggageServiceEntity entity) {
        return LuggageServiceProviderResponseDto.builder()
            .id(entity.getId())
            .name(entity.getName())
            .publicationStatus(entity.getPublicationStatus())
            .creationDate(entity.getCreationDate())
            .capacity(entity.getCapacity())
            .build();
    }

    // -------------------- RESPONSE for SAVE --------------------
    public LuggageServiceResponseDto toDetailDtoForSaveService(LuggageServiceEntity entity, User provider) {
        return LuggageServiceResponseDto.builder()
            .id(entity.getId())
            .name(entity.getName())
            .description(entity.getDescription())
            .locales(serviceLocaleMapper.mapEntityToReponse(entity.getLocales()))
            .capacity(entity.getCapacity())
            .basePrice(entity.getBasePrice())
            .images(entity.getImages())
            .provider(ProviderSummaryResponseDto.builder()
                .id(provider.getId())
                .name(provider.getName())
                .build())
            .sizePrices(entity.getSizePrices() != null
                ? entity.getSizePrices().stream()
                    .map(this::toSizePriceResponseDto)
                    .collect(Collectors.toList())
                : null)
            .serviceType(ServiceType.LUGGAGE)
            .build();
    }

    // -------------------- UPDATE --------------------
    public void updateEntityFromDto(LuggageServiceRequestDto dto, LuggageServiceEntity entity) {
        if (dto.getName() != null) entity.setName(dto.getName());
        if (dto.getDescription() != null) entity.setDescription(dto.getDescription());
        if (dto.getLocales() != null) entity.setLocales(serviceLocaleMapper.mapRequestToEntity(dto.getLocales()));
        if (dto.getCapacity() != null) entity.setCapacity(dto.getCapacity());
        if (dto.getBasePrice() != null) entity.setBasePrice(dto.getBasePrice());

        if (dto.getSizePrices() != null && !dto.getSizePrices().isEmpty()) {
            entity.getSizePrices().clear();
            entity.getSizePrices().addAll(dto.getSizePrices().stream()
                .map(this::toSizePriceEntity)
                .collect(Collectors.toList()));

            // *** collega i figli al padre ***
            entity.getSizePrices().forEach(sp -> sp.setLuggageService(entity));
        }
    }
    
    private OpeningHour toOpeningHourEntity(OpeningHourRequestDto dto, LuggageServiceEntity parent) {
        return OpeningHour.builder()
            .dayOfWeek(dto.getDayOfWeek())
            .openingTime(dto.getOpeningTime())
            .closingTime(dto.getClosingTime())
            .closed(dto.getClosed())
            .luggageService(parent)
            .build();
    }
    
    private OpeningHourResponseDto toOpeningHourResponseDto(OpeningHour entity) {
        return OpeningHourResponseDto.builder()
            .id(entity.getId())
            .dayOfWeek(entity.getDayOfWeek())
            .openingTime(entity.getOpeningTime())
            .closingTime(entity.getClosingTime())
            .closed(entity.getClosed())
            .build();
    }

    // -------------------- ENTITY → REQUEST (per modifiche) --------------------
   /* public LuggageServiceRequestDto fromEntityToRequestDto(LuggageServiceEntity entity) {
        return LuggageServiceRequestDto.builder()
            .name(entity.getName())
            .description(entity.getDescription())
            .capacity(entity.getCapacity())
            .basePrice(entity.getBasePrice())
            .images(entity.getImages())
            .locales(serviceLocaleMapper.mapRequestToEntity(entity.getLocales()))
            .sizePrices(entity.getSizePrices() != null
                ? entity.getSizePrices().stream()
                    .map(this::toSizePriceRequestDto)
                    .collect(Collectors.toList())
                : null)
            .build();
    }*/

   /* private LuggageSizePriceRequestDto toSizePriceRequestDto(LuggageSizePrice entity) {
        return LuggageSizePriceRequestDto.builder()
            .sizeLabel(entity.getSizeLabel())
            .pricePerHour(entity.getPricePerHour())
            .pricePerDay(entity.getPricePerDay())
            .description(entity.getDescription())
            .build();
    }*/
}
