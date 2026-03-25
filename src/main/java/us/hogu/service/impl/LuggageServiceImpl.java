package us.hogu.service.impl;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
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
import us.hogu.controller.dto.request.LuggageBookingEvent;
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
import us.hogu.controller.dto.response.LuggageBookingValidationResponseDto;
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
import us.hogu.model.enums.BookingStatus;
import us.hogu.model.enums.ServiceType;
import us.hogu.model.enums.UserRole;
import us.hogu.repository.jdbc.LuggageServiceJdbc;
import us.hogu.repository.jpa.LuggageBookingJpa;
import us.hogu.repository.jpa.LuggageServiceJpa;
import us.hogu.repository.jpa.UserJpa;
import us.hogu.service.intefaces.FileService;
import us.hogu.service.intefaces.LuggageService;
import us.hogu.service.mq.BookingProducer;
import us.hogu.service.redis.RedisAvailabilityService;

@RequiredArgsConstructor
@Service
public class LuggageServiceImpl implements LuggageService {

    private final LuggageServiceJpa luggageServiceJpa;
    private final LuggageBookingJpa luggageBookingJpa;
    private final UserJpa userJpa;
    private final LuggageServiceJdbc luggageServiceJdbc;
    private final FileService fileService;
    private final RedisAvailabilityService redisService;
    private final BookingProducer bookingProducer;

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

        List<ServiceLocaleResponseDto> localeDtos = entity.getLocales().stream()
                .map(loc -> ServiceLocaleResponseDto.builder()
                        .address(loc.getAddress())
                        .city(loc.getCity())
                        .country(loc.getCountry())
                        .state(loc.getState())
                        .language(loc.getLanguage())
                        .build())
                .collect(Collectors.toList());

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
                .serviceLocale(localeDtos)
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
            if (!redisService.checkLuggageAvailability(entity.getId(), entity.getCapacity())) {
                continue;
            }
            ServiceSummaryResponseDto dto = buildSummaryDto(entity);
            dto.setAvailable(true);
            dtoList.add(dto);
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

        // 1. Calculate Bags
        int bagsSmall = requestDto.getBagsSmall() != null ? requestDto.getBagsSmall() : 0;
        int bagsMedium = requestDto.getBagsMedium() != null ? requestDto.getBagsMedium() : 0;
        int bagsLarge = requestDto.getBagsLarge() != null ? requestDto.getBagsLarge() : 0;
        int totalBags = bagsSmall + bagsMedium + bagsLarge;

        if (totalBags <= 0) {
             throw new ValidationException(ErrorConstants.GENERIC_ERROR.name(), "Numero di bagagli non valido");
        }

        int maxCapacity = luggageService.getCapacity() != null ? luggageService.getCapacity() : 0;

        // 2. Redis Reservation (High Concurrency)
        boolean reserved = redisService.reserveLuggage(
                luggageService.getId(), 
                requestDto.getDropOffTime(), 
                requestDto.getPickUpTime(), 
                totalBags, 
                maxCapacity);

        if (!reserved) {
            throw new ValidationException(
                    ErrorConstants.INSUFFICIENT_AVAILABLE_SEATS.name(),
                    ErrorConstants.INSUFFICIENT_AVAILABLE_SEATS.getMessage());
        }

        // 3. Create Booking (Sync)
        LuggageBooking booking = buildBookingEntity(requestDto, user, luggageService);
        
        // Ricalcolo Prezzo (Backend Authoritative)
        BigDecimal calculatedTotal = calculateTotalAmount(
                requestDto.getDropOffTime(),
                requestDto.getPickUpTime(),
                bagsSmall,
                bagsMedium,
                bagsLarge,
                luggageService.getSizePrices()
        );
        booking.setTotalAmount(calculatedTotal);

        booking.setStatus(BookingStatus.PENDING);
        booking.setBillingFirstName(requestDto.getBillingFirstName());
        booking.setBillingLastName(requestDto.getBillingLastName());
        booking.setBillingTaxCode(requestDto.getFiscalCode());
        booking.setBillingVatNumber(requestDto.getTaxId());
        booking.setBillingAddress(requestDto.getBillingAddress());
        booking.setBillingEmail(requestDto.getBillingEmail());
        
        LuggageBooking savedBooking;
        try {
            savedBooking = luggageBookingJpa.save(booking);
        } catch (Exception e) {
            redisService.rollbackLuggage(
                luggageService.getId(), 
                requestDto.getDropOffTime(), 
                requestDto.getPickUpTime(), 
                totalBags);
            throw e;
        }

        // 4. Async Event for DB Persistence/Processing
        LuggageBookingEvent event = LuggageBookingEvent.builder()
                .userId(userId)
                .luggageServiceId(luggageService.getId())
                .bookingId(savedBooking.getId())
                .dropOffTime(requestDto.getDropOffTime())
                .pickUpTime(requestDto.getPickUpTime())
                .bagsSmall(bagsSmall)
                .bagsMedium(bagsMedium)
                .bagsLarge(bagsLarge)
                .totalAmount(savedBooking.getTotalAmount())
                .billingFirstName(user.getName())
                .billingLastName(user.getSurname())
                .build();
        
        try {
            bookingProducer.sendLuggageBookingRequest(event);
        } catch (Exception e) {
             // log error, booking is already saved in PENDING
        }

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

        Integer oldCapacity = entity.getCapacity();

        populateEntityFromDto(entity, requestDto);

        Integer newCapacity = entity.getCapacity();
        if (oldCapacity != null && newCapacity != null) {
            redisService.updateLuggageCapacity(serviceId, oldCapacity, newCapacity);
        }

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
	
	@Override
	@Transactional(readOnly = true)
	public Page<LuggageBookingResponseDto> getLuggageBookingsHistory(Long serviceId, Long providerId, Pageable pageable) {
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

		OffsetDateTime now = OffsetDateTime.now().withHour(0).withMinute(0).withSecond(0).withNano(0);
		Page<LuggageBooking> bookings = luggageBookingJpa.findPastBookings(serviceId, now, pageable);
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
            locale.setProvince(locDto.getProvince());
            locale.setCity(locDto.getCity());
            locale.setAddress(locDto.getAddress());
            locale.setPostalCode(locDto.getPostalCode());
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
        ServiceLocaleResponseDto locDto = null;
        if (entity.getLocales() != null && !entity.getLocales().isEmpty()) {
            ServiceLocale loc = entity.getLocales().get(0);
            locDto = ServiceLocaleResponseDto.builder()
                    .address(loc.getAddress())
                    .city(loc.getCity())
                    .country(loc.getCountry())
                    .language(loc.getLanguage())
                    .state(loc.getState())
                    .province(loc.getProvince())
                    .postalCode(loc.getPostalCode())
                    .build();
        }

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
                .locales(locDto != null ? List.of(locDto) : List.of())
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
        if (booking.getLuggageService() == null) {
             throw new ValidationException(ErrorConstants.GENERIC_ERROR.name(), "Integrità dati violata: Prenotazione " + booking.getId() + " senza servizio associato");
        }
        String firstName = booking.getBillingFirstName();
        String lastName = booking.getBillingLastName();
        if ((firstName == null || firstName.isBlank()) && booking.getUser() != null) {
            firstName = booking.getUser().getName();
        }
        if ((lastName == null || lastName.isBlank()) && booking.getUser() != null) {
            lastName = booking.getUser().getSurname();
        }
        String fullName = null;
        if (firstName != null && !firstName.isBlank() && lastName != null && !lastName.isBlank()) {
            fullName = firstName + " " + lastName;
        } else if (firstName != null && !firstName.isBlank()) {
            fullName = firstName;
        } else if (lastName != null && !lastName.isBlank()) {
            fullName = lastName;
        }
        return LuggageBookingResponseDto.builder()
                .id(booking.getId())
                .serviceType(ServiceType.LUGGAGE)
                .serviceId(booking.getLuggageService().getId())
                .serviceName(booking.getLuggageService().getName())
                .customerFirstName(firstName)
                .customerLastName(lastName)
                .customerName(fullName)
                .status(booking.getStatus())
                .totalAmount(booking.getTotalAmount())
                .creationDate(booking.getCreationDate())
                .pickUpTime(booking.getPickUpTime())
                .dropOffTime(booking.getDropOffTime())
                .bagsSmall(booking.getBagsSmall())
                .bagsMedium(booking.getBagsMedium())
                .bagsLarge(booking.getBagsLarge())
                .specialRequests(booking.getSpecialRequests())
                .build();
    }

    private LuggageBooking buildBookingEntity(LuggageBookingRequestDto dto, User user, LuggageServiceEntity service) {
        return LuggageBooking.builder()
                .user(user)
                .luggageService(service)
                .dropOffTime(dto.getDropOffTime())
                .pickUpTime(dto.getPickUpTime())
                .bagsSmall(dto.getBagsSmall() != null ? dto.getBagsSmall() : 0)
                .bagsMedium(dto.getBagsMedium() != null ? dto.getBagsMedium() : 0)
                .bagsLarge(dto.getBagsLarge() != null ? dto.getBagsLarge() : 0)
                .specialRequests(dto.getSpecialRequests())
                .totalAmount(dto.getTotalAmount())
                .build();
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

    // ===================================================================
    // PRICE CALCULATION METHODS
    // ===================================================================

    private BigDecimal calculateTotalAmount(OffsetDateTime dropOffTime, OffsetDateTime pickUpTime,
                                            int bagsSmall, int bagsMedium, int bagsLarge,
                                            List<LuggageSizePrice> sizePrices) {
        if (dropOffTime == null || pickUpTime == null || sizePrices == null) {
            return BigDecimal.ZERO;
        }

        long diffInMinutes = Duration.between(dropOffTime, pickUpTime).toMinutes();
        if (diffInMinutes <= 0) {
            return BigDecimal.ZERO;
        }

        // Round up to the next hour
        long totalHours = (diffInMinutes + 59) / 60;
        long fullDays = totalHours / 24;
        long remainingHours = totalHours % 24;

        BigDecimal totalCost = BigDecimal.ZERO;

        // SMALL
        if (bagsSmall > 0) {
            totalCost = totalCost.add(calculateBagCost("SMALL", bagsSmall, fullDays, remainingHours, sizePrices));
        }
        // MEDIUM
        if (bagsMedium > 0) {
            totalCost = totalCost.add(calculateBagCost("MEDIUM", bagsMedium, fullDays, remainingHours, sizePrices));
        }
        // LARGE
        if (bagsLarge > 0) {
            totalCost = totalCost.add(calculateBagCost("LARGE", bagsLarge, fullDays, remainingHours, sizePrices));
        }

        return totalCost.setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal calculateBagCost(String sizeLabel, int count, long fullDays, long remainingHours, List<LuggageSizePrice> sizePrices) {
        LuggageSizePrice priceConfig = sizePrices.stream()
                .filter(sp -> sp.getSizeLabel() != null && sp.getSizeLabel().equalsIgnoreCase(sizeLabel))
                .findFirst()
                .orElse(null);

        if (priceConfig == null) {
            return BigDecimal.ZERO;
        }

        BigDecimal pPerHour = priceConfig.getPricePerHour() != null ? priceConfig.getPricePerHour() : BigDecimal.ZERO;
        BigDecimal pPerDay = priceConfig.getPricePerDay() != null ? priceConfig.getPricePerDay() : BigDecimal.ZERO;

        // Normalization logic: if daily is 0 and hourly > 0, set daily = hourly * 24
        if (pPerDay.compareTo(BigDecimal.ZERO) == 0 && pPerHour.compareTo(BigDecimal.ZERO) > 0) {
            pPerDay = pPerHour.multiply(BigDecimal.valueOf(24));
        }
        
        BigDecimal finalHourly = pPerHour.compareTo(BigDecimal.ZERO) > 0 ? pPerHour : pPerDay;

        // Cost for full days
        BigDecimal costFullDays = pPerDay.multiply(BigDecimal.valueOf(fullDays));

        // Cost for remaining hours
        BigDecimal costRemainingHourly = finalHourly.multiply(BigDecimal.valueOf(remainingHours));
        // Cap at daily rate
        BigDecimal costRemaining = costRemainingHourly.min(pPerDay);

        BigDecimal costPerBag = costFullDays.add(costRemaining);
        
        return costPerBag.multiply(BigDecimal.valueOf(count));
    }

    @Override
    @Transactional(readOnly = true)
    public LuggageBookingValidationResponseDto validateLuggageBookingByCode(Long providerId, String code) {
        if (code == null || code.trim().isEmpty()) {
            return LuggageBookingValidationResponseDto.builder()
                    .valid(false)
                    .serviceType(ServiceType.LUGGAGE.name())
                    .build();
        }

        LuggageBooking booking = luggageBookingJpa.findByBookingCodeIgnoreCase(code)
                .orElseThrow(() -> new ValidationException(
                        ErrorConstants.BOOKING_LUGGAGE_NOT_FOUND.name(),
                        ErrorConstants.BOOKING_LUGGAGE_NOT_FOUND.getMessage()));

        if (booking.getLuggageService() == null || booking.getLuggageService().getUser() == null) {
            throw new ValidationException(
                    ErrorConstants.BOOKING_LUGGAGE_NOT_FOUND.name(),
                    ErrorConstants.BOOKING_LUGGAGE_NOT_FOUND.getMessage());
        }

        Long ownerId = booking.getLuggageService().getUser().getId();
        if (ownerId == null || !ownerId.equals(providerId)) {
            throw new ValidationException(
                    ErrorConstants.UNAUTHORIZED.name(),
                    "Non sei autorizzato a validare questa prenotazione.");
        }

        boolean statusValid = booking.getStatus() == BookingStatus.FULL_PAYMENT_COMPLETED
                || booking.getStatus() == BookingStatus.COMPLETED;

        String firstName = booking.getBillingFirstName();
        String lastName = booking.getBillingLastName();
        if ((firstName == null || firstName.isBlank()) && booking.getUser() != null) {
            firstName = booking.getUser().getName();
        }
        if ((lastName == null || lastName.isBlank()) && booking.getUser() != null) {
            lastName = booking.getUser().getSurname();
        }
        String fullName = null;
        if (firstName != null && !firstName.isBlank() && lastName != null && !lastName.isBlank()) {
            fullName = firstName + " " + lastName;
        } else if (firstName != null && !firstName.isBlank()) {
            fullName = firstName;
        } else if (lastName != null && !lastName.isBlank()) {
            fullName = lastName;
        }

        return LuggageBookingValidationResponseDto.builder()
                .valid(statusValid)
                .serviceType(ServiceType.LUGGAGE.name())
                .bookingId(booking.getId())
                .firstName(firstName)
                .lastName(lastName)
                .fullName(fullName)
                .serviceName(booking.getLuggageService().getName())
                .dropOffTime(booking.getDropOffTime() != null ? booking.getDropOffTime().toString() : null)
                .pickUpTime(booking.getPickUpTime() != null ? booking.getPickUpTime().toString() : null)
                .bagsSmall(booking.getBagsSmall())
                .bagsMedium(booking.getBagsMedium())
                .bagsLarge(booking.getBagsLarge())
                .totalAmount(booking.getTotalAmount() != null ? booking.getTotalAmount().toPlainString() : null)
                .build();
    }
}
