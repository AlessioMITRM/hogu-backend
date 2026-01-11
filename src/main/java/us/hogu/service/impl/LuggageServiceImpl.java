package us.hogu.service.impl;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import lombok.RequiredArgsConstructor;
import us.hogu.common.constants.ErrorConstants;
import us.hogu.common.util.ImageUtils;
import us.hogu.controller.dto.request.LuggageBookingRequestDto;
import us.hogu.controller.dto.request.LuggageSearchRequestDto;
import us.hogu.controller.dto.request.LuggageServiceRequestDto;
import us.hogu.controller.dto.request.LuggageSizePriceRequestDto;
import us.hogu.controller.dto.request.OpeningHourRequestDto;
import us.hogu.controller.dto.request.ServiceLocaleRequestDto;
import us.hogu.controller.dto.response.ClubInfoStatsDto;
import us.hogu.controller.dto.response.InfoStatsDto;
import us.hogu.controller.dto.response.LuggageBookingResponseDto;
import us.hogu.controller.dto.response.LuggageSearchResultResponseDto;
import us.hogu.controller.dto.response.LuggageServiceAdminResponseDto;
import us.hogu.controller.dto.response.LuggageServiceDetailResponseDto;
import us.hogu.controller.dto.response.LuggageServiceProviderResponseDto;
import us.hogu.controller.dto.response.LuggageServiceResponseDto;
import us.hogu.controller.dto.response.LuggageSizePriceResponseDto;
import us.hogu.controller.dto.response.OpeningHourResponseDto;
import us.hogu.controller.dto.response.ServiceLocaleResponseDto;
import us.hogu.controller.dto.response.ServiceSummaryResponseDto;
import us.hogu.exception.ValidationException;
import us.hogu.model.LuggageBooking;
import us.hogu.model.LuggageServiceEntity;
import us.hogu.model.LuggageSizePrice;
import us.hogu.model.OpeningHour;
import us.hogu.model.ServiceLocale;
import us.hogu.model.User;
import us.hogu.model.enums.ServiceType;
import us.hogu.model.enums.UserRole;
import us.hogu.repository.jdbc.LuggageServiceJdbc;
import us.hogu.repository.jpa.LuggageBookingJpa;
import us.hogu.repository.jpa.LuggageServiceJpa;
import us.hogu.repository.jpa.UserJpa;
import us.hogu.service.intefaces.FileService;
import us.hogu.service.intefaces.LuggageService;

@RequiredArgsConstructor
@Service
public class LuggageServiceImpl implements LuggageService {

    private final LuggageServiceJpa luggageServiceJpa;
    private final LuggageBookingJpa luggageBookingJpa;
    private final UserJpa userJpa;
    private final LuggageServiceJdbc luggageServiceJdbc;
    private final FileService fileService;

    private static final BigDecimal ZERO = BigDecimal.ZERO;
    private static final int MONEY_SCALE = 2;
    private static final RoundingMode MONEY_ROUNDING = RoundingMode.HALF_UP;

    
    @Override
    @Transactional(readOnly = true)
    public InfoStatsDto getInfo(Long providerId) {
        LuggageServiceEntity entity = luggageServiceJpa.findByProviderIdForSingleService(providerId)
                .orElseThrow(() -> new ValidationException(
                        ErrorConstants.SERVICE_LUGGAGE_NOT_FOUND.name(),
                        ErrorConstants.SERVICE_LUGGAGE_NOT_FOUND.getMessage()));

        InfoStatsDto infoStats = luggageServiceJpa.getInfoStatsByProviderId(providerId);
        infoStats.setServiceId(entity.getId());

        return infoStats;
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ServiceSummaryResponseDto> getAllActiveLuggageServices(Pageable pageable) {
        Page<LuggageServiceEntity> entities = luggageServiceJpa.findActiveSummaries(pageable);
        List<ServiceSummaryResponseDto> dtoList = new ArrayList<>();
        for (LuggageServiceEntity entity : entities.getContent()) {
            dtoList.add(buildSummaryDto(entity));
        }
        return new PageImpl<>(dtoList, pageable, entities.getTotalElements());
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ServiceSummaryResponseDto> getAllLuggageServicesByProvider(Long providerId, Pageable pageable) {
        Page<LuggageServiceEntity> entities = luggageServiceJpa.findByProviderId(providerId, pageable);
        List<ServiceSummaryResponseDto> content = entities.getContent()
                .stream()
                .map(this::buildSummaryDto)
                .collect(Collectors.toList());
        return new PageImpl<>(content, pageable, entities.getTotalElements());
    }

    @Override
    @Transactional(readOnly = true)
    public LuggageServiceResponseDto getLuggageServiceDetail(Long serviceId) {
        String language = LocaleContextHolder.getLocale().getLanguage();
        LuggageServiceEntity entity = luggageServiceJpa.findDetailById(serviceId, language)
                .orElseThrow(() -> new ValidationException(
                        ErrorConstants.SERVICE_LUGGAGE_NOT_FOUND.name(),
                        ErrorConstants.SERVICE_LUGGAGE_NOT_FOUND.getMessage()));

        ServiceLocale loc = entity.getLocales().get(0);
        ServiceLocaleResponseDto locDto = ServiceLocaleResponseDto.builder()
                .address(loc.getAddress())
                .city(loc.getCity())
                .country(loc.getCountry())
                .language(loc.getLanguage())
                .build();

        List<LuggageSizePriceResponseDto> sizePrices = new ArrayList<>();
        for (LuggageSizePrice sp : entity.getSizePrices()) {
            LuggageSizePriceResponseDto spDto = new LuggageSizePriceResponseDto();
            spDto.setSizeLabel(sp.getSizeLabel());
            spDto.setPricePerDay(sp.getPricePerDay());
            spDto.setPricePerHour(sp.getPricePerHour());
            spDto.setDescription(sp.getDescription());
            sizePrices.add(spDto);
        }

        List<OpeningHourResponseDto> openingHours = new ArrayList<>();
        for (OpeningHour oh : entity.getOpeningHours()) {
            OpeningHourResponseDto ohDto = OpeningHourResponseDto.builder()
                    .dayOfWeek(oh.getDayOfWeek())
                    .openingTime(oh.getOpeningTime())
                    .closingTime(oh.getClosingTime())
                    .closed(oh.getClosed())
                    .build();
            openingHours.add(ohDto);
        }

        return LuggageServiceResponseDto.builder()
                .name(entity.getName())
                .images(entity.getImages())
                .available(true)
                .serviceLocale(List.of(locDto))
                .basePrice(entity.getBasePrice())
                .sizePrices(sizePrices)
                .openingHours(openingHours)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ServiceSummaryResponseDto> searchLuggageServices(String searchTerm, Pageable pageable) {
        Page<LuggageServiceEntity> entities = luggageServiceJpa.findActiveBySearch(searchTerm, pageable);
        List<ServiceSummaryResponseDto> dtoList = new ArrayList<>();
        for (LuggageServiceEntity entity : entities.getContent()) {
            dtoList.add(buildSummaryDto(entity));
        }
        return new PageImpl<>(dtoList, pageable, entities.getTotalElements());
    }

    @Override
    @Transactional(readOnly = true)
    public Page<LuggageBookingResponseDto> getUserLuggageBookings(Long userId, Pageable pageable) {
        userJpa.findById(userId)
                .orElseThrow(() -> new ValidationException(
                        ErrorConstants.USER_NOT_FOUND.name(),
                        ErrorConstants.USER_NOT_FOUND.getMessage()));

        Page<LuggageBooking> bookings = luggageBookingJpa.findByUserId(userId, pageable);
        List<LuggageBookingResponseDto> content = bookings.getContent()
                .stream()
                .map(this::buildBookingResponseDto)
                .collect(Collectors.toList());

        return new PageImpl<>(content, pageable, bookings.getTotalElements());
    }

    @Override
    @Transactional
    public LuggageBookingResponseDto createLuggageBooking(Long userId, LuggageBookingRequestDto requestDto) {
        User user = userJpa.findById(userId)
                .orElseThrow(() -> new ValidationException(
                        ErrorConstants.USER_NOT_FOUND.name(),
                        ErrorConstants.USER_NOT_FOUND.getMessage()));

        LuggageServiceEntity luggageService = luggageServiceJpa.findById(requestDto.getLuggageServiceId())
                .orElseThrow(() -> new ValidationException(
                        ErrorConstants.SERVICE_LUGGAGE_NOT_FOUND.name(),
                        ErrorConstants.SERVICE_LUGGAGE_NOT_FOUND.getMessage()));

        checkLuggageAvailability(luggageService, requestDto.getReservationTime(), requestDto.getNumberOfPeople());

        LuggageBooking booking = buildBookingEntity(requestDto, user, luggageService);
        LuggageBooking savedBooking = luggageBookingJpa.save(booking);

        return buildBookingResponseDto(savedBooking);
    }

    @Override
    @Transactional
    public LuggageServiceDetailResponseDto createLuggageService(Long providerId,
                                                                LuggageServiceRequestDto requestDto,
                                                                List<MultipartFile> images) throws IOException {
        User provider = userJpa.findById(providerId)
                .orElseThrow(() -> new ValidationException(
                        ErrorConstants.PROVIDER_NOT_FOUND.name(),
                        ErrorConstants.PROVIDER_NOT_FOUND.getMessage()));

        LuggageServiceEntity entity = new LuggageServiceEntity();
        populateEntityFromDto(entity, requestDto);

        entity.setUser(provider);
        entity.setPublicationStatus(requestDto.getPublicationStatus());
        entity.setImages(new ArrayList<>());

        BigDecimal calculatedBasePrice = calculateBasePrice(requestDto.getSizePrices());
        entity.setBasePrice(calculatedBasePrice);

        LuggageServiceEntity saved = luggageServiceJpa.save(entity);

        Path basePath = Paths.get(ImageUtils.STORAGE_ROOT,
                ServiceType.LUGGAGE.name().toLowerCase(),
                saved.getId().toString());

        fileService.uploadImagesPathCustom(basePath, saved.getImages(), images);

        saved = luggageServiceJpa.save(saved);

        return buildDetailResponseDto(saved, provider);
    }

    @Override
    @Transactional
    public LuggageServiceDetailResponseDto updateLuggageService(Long providerId,
                                                                Long serviceId,
                                                                LuggageServiceRequestDto requestDto,
                                                                List<MultipartFile> images) throws Exception {
        LuggageServiceEntity entity = luggageServiceJpa.findById(serviceId)
                .orElseThrow(() -> new ValidationException(
                        ErrorConstants.SERVICE_LUGGAGE_NOT_FOUND.name(),
                        ErrorConstants.SERVICE_LUGGAGE_NOT_FOUND.getMessage()));

        if (!entity.getUser().getId().equals(providerId)) {
            throw new ValidationException(
                    ErrorConstants.UNAUTHORIZED.name(),
                    "Non sei autorizzato a modificare questo deposito.");
        }

        populateEntityFromDto(entity, requestDto);

        BigDecimal calculatedBasePrice = calculateBasePrice(requestDto.getSizePrices());

        entity.setBasePrice(calculatedBasePrice);
        entity.setPublicationStatus(requestDto.getPublicationStatus());

        LuggageServiceEntity updated = luggageServiceJpa.save(entity);

        Path basePath = Paths.get(ImageUtils.STORAGE_ROOT,
                ServiceType.LUGGAGE.name().toLowerCase(),
                updated.getId().toString());

        fileService.updateImagesPathCustom(basePath, updated.getImages(), images);

        updated = luggageServiceJpa.save(updated);

        // MODIFICA RICHIESTA: usa buildDetailResponseDto invece di buildProviderResponseDto
        return buildDetailResponseDto(updated, entity.getUser());
    }

    @Override
    @Transactional(readOnly = true)
    public LuggageServiceDetailResponseDto getLuggageServiceByIdAndProvider(Long serviceId, Long providerId) {
        LuggageServiceEntity entity = luggageServiceJpa.findByIdAndUserId(serviceId, providerId)
                .orElseThrow(() -> new ValidationException(
                        ErrorConstants.SERVICE_LUGGAGE_NOT_FOUND.name(),
                        "Deposito non trovato o non appartiene al provider."));

        // MODIFICA RICHIESTA: usa buildDetailResponseDto invece di buildProviderResponseDto
        return buildDetailResponseDto(entity, entity.getUser());
    }

    @Override
    @Transactional(readOnly = true)
    public List<LuggageServiceProviderResponseDto> getProviderLuggageServices(Long providerId) {
        List<LuggageServiceEntity> entities = luggageServiceJpa.findAllByUserId(providerId);
        return entities.stream()
                .map(this::buildProviderResponseDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public Page<LuggageBookingResponseDto> getLuggageBookings(Long serviceId, Long providerId, Pageable pageable) {
        LuggageServiceEntity luggageService = luggageServiceJpa.findById(serviceId)
                .orElseThrow(() -> new ValidationException(
                        ErrorConstants.SERVICE_LUGGAGE_NOT_FOUND.name(),
                        ErrorConstants.SERVICE_LUGGAGE_NOT_FOUND.getMessage()));

        User provider = userJpa.findById(providerId)
                .orElseThrow(() -> new ValidationException(
                        ErrorConstants.USER_NOT_FOUND.name(),
                        ErrorConstants.USER_NOT_FOUND.getMessage()));

        if (provider.getRole() != UserRole.PROVIDER) {
            throw new ValidationException(
                    ErrorConstants.USER_ROLE_INVALID.name(),
                    ErrorConstants.USER_ROLE_INVALID.getMessage());
        }

        Page<LuggageBooking> bookings = luggageBookingJpa.findByLuggageServiceId(serviceId, pageable);
        List<LuggageBookingResponseDto> content = bookings.getContent()
                .stream()
                .map(this::buildBookingResponseDto)
                .collect(Collectors.toList());

        return new PageImpl<>(content, pageable, bookings.getTotalElements());
    }

    // ADMIN METHODS
    @Override
    @Transactional(readOnly = true)
    public List<LuggageServiceAdminResponseDto> getAllLuggageServicesForAdmin() {
        List<LuggageServiceEntity> entities = luggageServiceJpa.findAllForAdmin();
        return entities.stream()
                .map(this::buildAdminResponseDto)
                .collect(Collectors.toList());
    }

    @Override
    public Page<LuggageSearchResultResponseDto> searchNative(LuggageSearchRequestDto request, Pageable pageable) {
        return luggageServiceJdbc.searchNative(request, pageable);
    }

    @Override
    @Transactional
    public void approveLuggageService(Long serviceId) {
        LuggageServiceEntity luggageService = luggageServiceJpa.findById(serviceId)
                .orElseThrow(() -> new ValidationException(
                        ErrorConstants.SERVICE_LUGGAGE_NOT_FOUND.name(),
                        ErrorConstants.SERVICE_LUGGAGE_NOT_FOUND.getMessage()));

        luggageService.setPublicationStatus(true);
        luggageServiceJpa.save(luggageService);
    }

    // ===================================================================
    // METODI PRIVATI DI SUPPORTO
    // ===================================================================

    private void populateEntityFromDto(LuggageServiceEntity entity, LuggageServiceRequestDto dto) {
        entity.setName(dto.getName());
        entity.setDescription(dto.getDescription());
        entity.setCapacity(dto.getCapacity());

        // Locales - modifica la collezione esistente
        if (entity.getLocales() == null) {
            entity.setLocales(new ArrayList<>());
        }
        entity.getLocales().clear();
        if (dto.getLocales() != null && !dto.getLocales().isEmpty()) {
            ServiceLocaleRequestDto locDto = dto.getLocales().get(0);
            ServiceLocale locale = new ServiceLocale();
            locale.setServiceType(ServiceType.LUGGAGE);
            locale.setLanguage(locDto.getLanguage() != null ? locDto.getLanguage() : "it");
            locale.setCountry(locDto.getCountry() != null ? locDto.getCountry() : "Italia");
            locale.setState(locDto.getState());
            locale.setCity(locDto.getCity());
            locale.setAddress(locDto.getAddress());
            entity.getLocales().add(locale);
        }

        // OpeningHours - modifica la collezione esistente
        if (entity.getOpeningHours() == null) {
            entity.setOpeningHours(new ArrayList<>());
        }
        entity.getOpeningHours().clear();
        if (dto.getOpeningHours() != null) {
            for (OpeningHourRequestDto ohDto : dto.getOpeningHours()) {
                OpeningHour oh = new OpeningHour();
                oh.setDayOfWeek(ohDto.getDayOfWeek());
                oh.setOpeningTime(ohDto.getOpeningTime());
                oh.setClosingTime(ohDto.getClosingTime());
                oh.setClosed(ohDto.getClosed() != null && ohDto.getClosed());
                oh.setLuggageService(entity);
                entity.getOpeningHours().add(oh);
            }
        }

        // SizePrices - modifica la collezione esistente
        if (entity.getSizePrices() == null) {
            entity.setSizePrices(new ArrayList<>());
        }
        entity.getSizePrices().clear();
        if (dto.getSizePrices() != null) {
            for (LuggageSizePriceRequestDto spDto : dto.getSizePrices()) {
                LuggageSizePrice sp = new LuggageSizePrice();
                sp.setSizeLabel(spDto.getSizeLabel());
                sp.setPricePerDay(spDto.getPricePerDay());
                sp.setPricePerHour(spDto.getPricePerHour());
                sp.setDescription(spDto.getDescription());
                sp.setLuggageService(entity);
                entity.getSizePrices().add(sp);
            }
        }

        entity.setPublicationStatus(dto.getPublicationStatus());
    }

    private BigDecimal calculateBasePrice(List<LuggageSizePriceRequestDto> sizePrices) {
        if (sizePrices == null || sizePrices.isEmpty()) {
            return ZERO;
        }

        return sizePrices.stream()
                .filter(sp -> sp.getPricePerDay() != null)
                .map(LuggageSizePriceRequestDto::getPricePerDay)
                .filter(price -> price != null && price.compareTo(ZERO) > 0)
                .min(BigDecimal::compareTo)
                .orElse(ZERO);
    }

    private LuggageServiceDetailResponseDto buildDetailResponseDto(LuggageServiceEntity entity, User provider) {
        ServiceLocale loc = entity.getLocales().get(0);
        ServiceLocaleResponseDto locDto = ServiceLocaleResponseDto.builder()
                .address(loc.getAddress())
                .city(loc.getCity())
                .country(loc.getCountry())
                .language(loc.getLanguage())
                .build();

        List<LuggageSizePriceResponseDto> sizePrices = new ArrayList<>();
        for (LuggageSizePrice sp : entity.getSizePrices()) {
            LuggageSizePriceResponseDto spDto = new LuggageSizePriceResponseDto();
            spDto.setSizeLabel(sp.getSizeLabel());
            spDto.setPricePerDay(sp.getPricePerDay());
            spDto.setPricePerHour(sp.getPricePerHour());
            spDto.setDescription(sp.getDescription());
            sizePrices.add(spDto);
        }

        List<OpeningHourResponseDto> openingHours = new ArrayList<>();
        for (OpeningHour oh : entity.getOpeningHours()) {
            OpeningHourResponseDto ohDto = OpeningHourResponseDto.builder()
                    .dayOfWeek(oh.getDayOfWeek())
                    .openingTime(oh.getOpeningTime())
                    .closingTime(oh.getClosingTime())
                    .closed(oh.getClosed())
                    .build();
            openingHours.add(ohDto);
        }

        return LuggageServiceDetailResponseDto.builder()
                .name(entity.getName())
                .images(entity.getImages())
                .description(entity.getDescription())
                .publicationStatus(entity.getPublicationStatus())
                .locales(List.of(locDto))
                .basePrice(entity.getBasePrice())
                .sizePrices(sizePrices)
                .openingHours(openingHours)
                .capacity(entity.getCapacity())
                .build();
    }

    private LuggageServiceProviderResponseDto buildProviderResponseDto(LuggageServiceEntity entity) {
        LuggageServiceProviderResponseDto dto = new LuggageServiceProviderResponseDto();
        dto.setId(entity.getId());
        dto.setName(entity.getName());
        dto.setDescription(entity.getDescription());
        dto.setCapacity(entity.getCapacity());
        dto.setBasePrice(entity.getBasePrice());
        dto.setPublicationStatus(entity.getPublicationStatus());

        if (!entity.getLocales().isEmpty()) {
            ServiceLocale loc = entity.getLocales().get(0);
            dto.setCity(loc.getCity());
            dto.setState(loc.getState());
            dto.setAddress(loc.getAddress());
        }

        dto.setImages(dto.getImages());

        return dto;
    }

    private ServiceSummaryResponseDto buildSummaryDto(LuggageServiceEntity entity) {
        ServiceSummaryResponseDto dto = new ServiceSummaryResponseDto();
        dto.setId(entity.getId());
        dto.setName(entity.getName());
        // ... altri campi necessari secondo la tua definizione del DTO
        return dto;
    }

    private LuggageBookingResponseDto buildBookingResponseDto(LuggageBooking booking) {
        LuggageBookingResponseDto dto = new LuggageBookingResponseDto();
        // ... mapping manuale secondo la tua struttura del DTO
        return dto;
    }

    private LuggageBooking buildBookingEntity(LuggageBookingRequestDto dto, User user, LuggageServiceEntity service) {
        LuggageBooking booking = new LuggageBooking();
        // ... set campi secondo necessità
        return booking;
    }

    private LuggageServiceResponseDto buildServiceResponseDto(LuggageServiceEntity entity) {
        LuggageServiceResponseDto dto = new LuggageServiceResponseDto();
        // ... mapping secondo necessità
        return dto;
    }

    private LuggageServiceAdminResponseDto buildAdminResponseDto(LuggageServiceEntity entity) {
        LuggageServiceAdminResponseDto dto = new LuggageServiceAdminResponseDto();
        // ... mapping secondo necessità
        return dto;
    }

    private void checkLuggageAvailability(LuggageServiceEntity luggageService,
                                         OffsetDateTime reservationTime,
                                         Integer numberOfPeople) {
        // TODO: implementare la logica reale di controllo disponibilità
        throw new ValidationException(
                ErrorConstants.INSUFFICIENT_AVAILABLE_SEATS.name(),
                ErrorConstants.INSUFFICIENT_AVAILABLE_SEATS.getMessage());
    }

    @Override
    public List<LuggageServiceProviderResponseDto> getLuggageServicesByProviderId(Long providerId) {
        // TODO: implementazione reale
        return Collections.emptyList();
    }
}