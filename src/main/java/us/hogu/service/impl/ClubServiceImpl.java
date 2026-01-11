package us.hogu.service.impl;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.management.ServiceNotFoundException;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.util.StringBuilders;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import us.hogu.common.constants.ErrorConstants;
import us.hogu.common.util.ImageUtils;
import us.hogu.controller.dto.request.ClubBookingRequestDto;
import us.hogu.controller.dto.request.ClubServiceRequestDto;
import us.hogu.controller.dto.request.EventClubServiceRequestDto;
import us.hogu.controller.dto.request.EventCreateRequestDto;
import us.hogu.controller.dto.response.ClubBookingResponseDto;
import us.hogu.controller.dto.response.ClubManagementResponseDto;
import us.hogu.controller.dto.response.ClubServiceResponseDto;
import us.hogu.controller.dto.response.EventClubServiceResponseDto;
import us.hogu.controller.dto.response.EventPricingConfigurationResponseDto;
import us.hogu.controller.dto.response.EventPublicResponseDto;
import us.hogu.controller.dto.response.ClubInfoStatsDto;
import us.hogu.controller.dto.response.ServiceDetailResponseDto;
import us.hogu.controller.dto.response.ServiceLocaleResponseDto;
import us.hogu.controller.dto.response.ServiceSummaryResponseDto;
import us.hogu.converter.BookingMapper;
import us.hogu.converter.ClubServiceMapper;
import us.hogu.converter.EventPricingConfigurationMapper;
import us.hogu.converter.ServiceLocaleMapper;
import us.hogu.exception.UserNotFoundException;
import us.hogu.exception.ValidationException;
import us.hogu.model.ClubBooking;
import us.hogu.model.ClubServiceEntity;
import us.hogu.model.EventClubServiceEntity;
import us.hogu.model.EventPricingConfiguration;
import us.hogu.model.ServiceLocale;
import us.hogu.model.User;
import us.hogu.model.enums.BookingStatus;
import us.hogu.model.enums.ServiceType;
import us.hogu.model.enums.VerificationStatusServiceEY;
import us.hogu.repository.jdbc.ClubEventJdbc;
import us.hogu.repository.jpa.ClubBookingJpa;
import us.hogu.repository.jpa.ClubServiceJpa;
import us.hogu.repository.jpa.EventClubServiceRepository;

import us.hogu.repository.jpa.UserJpa;
import us.hogu.repository.jpa.UserServiceVerificationJpa;
import us.hogu.repository.projection.ClubManagementProjection;
import us.hogu.repository.projection.ClubSummaryProjection;
import us.hogu.service.intefaces.ClubService;
import us.hogu.service.intefaces.CommissionService;
import us.hogu.service.intefaces.EventPricingConfigurationService;
import us.hogu.service.intefaces.FileService;
import us.hogu.service.intefaces.PayPalService;
import us.hogu.service.intefaces.StripeService;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Service
public class ClubServiceImpl implements ClubService {
	private final ClubServiceJpa clubServiceJpa;
	private final ClubBookingJpa clubBookingJpa;
	private final EventClubServiceRepository eventClubServiceRepository;
	private final UserJpa userJpa;
	private final ClubEventJdbc clubEventJdbc;
	private final UserServiceVerificationJpa userServiceVerificationJpa;
	private final FileService fileService;
	private final EventPricingConfigurationService eventPricingConfigurationService;
	private final ClubServiceMapper clubServiceMapper;
	private final BookingMapper bookingMapper;
	private final ServiceLocaleMapper serviceLocaleMapper;
	private final EventPricingConfigurationMapper eventPricingConfigurationMapper;

	@Override
	@Transactional(readOnly = true)
	public Page<ClubBookingResponseDto> getClubBookings(Long providerId, Long clubId, Pageable pageable) {
		boolean existClub = clubServiceJpa.existsByIdAndUserId(clubId, providerId);
		if (!existClub) {
			throw new ValidationException(ErrorConstants.CLUB_NOT_FOUND_OR_NOT_AUTHORIZED.name(),
					ErrorConstants.CLUB_NOT_FOUND_OR_NOT_AUTHORIZED.getMessage());
		}

		Page<ClubBooking> bookings = clubBookingJpa.findByclubServiceIdAndStatus(clubId, BookingStatus.PENDING,
				pageable);

		List<ClubBookingResponseDto> dtoList = new ArrayList<ClubBookingResponseDto>();
		for (ClubBooking entity : bookings.getContent()) {
			ClubBookingResponseDto dto = ClubBookingResponseDto.builder().id(entity.getId()).eventId(entity.getId())
					.bookingFullName(entity.getBillingFirstName() + StringUtils.SPACE + entity.getBillingLastName())
					.reservationTime(entity.getReservationTime()).numberOfPeople(entity.getNumberOfPeople())
					.totalAmount(entity.getTotalAmount()).status(entity.getStatus()).table(entity.getTable()).build();

			dtoList.add(dto);
		}

		return new PageImpl<>(dtoList, pageable, bookings.getTotalElements());
	}

	@Override
	@Transactional(readOnly = true)
	public Page<ClubBookingResponseDto> getClubBookingsPending(Long providerId, Long clubId, Pageable pageable) {
		boolean existClub = clubServiceJpa.existsByIdAndUserId(clubId, providerId);
		if (!existClub) {
			throw new ValidationException(ErrorConstants.CLUB_NOT_FOUND_OR_NOT_AUTHORIZED.name(),
					ErrorConstants.CLUB_NOT_FOUND_OR_NOT_AUTHORIZED.getMessage());
		}

		Page<ClubBooking> bookings = clubBookingJpa.findByclubServiceIdAndStatus(clubId, BookingStatus.PENDING,
				pageable);

		List<ClubBookingResponseDto> dtoList = new ArrayList<ClubBookingResponseDto>();
		for (ClubBooking entity : bookings.getContent()) {
			ClubBookingResponseDto dto = ClubBookingResponseDto.builder().id(entity.getId()).eventId(entity.getId())
					.bookingFullName(entity.getBillingFirstName() + StringUtils.SPACE + entity.getBillingLastName())
					.reservationTime(entity.getReservationTime()).numberOfPeople(entity.getNumberOfPeople())
					.totalAmount(entity.getTotalAmount()).build();

			dtoList.add(dto);
		}

		return new PageImpl<>(dtoList, pageable, bookings.getTotalElements());
	}

	@Override
	@Transactional(readOnly = true)
	public ClubInfoStatsDto getInfo(Long providerId) {
		return clubServiceJpa.getInfoStatsByProviderId(providerId);
	}

	@Override
	@Transactional(readOnly = true)
	public Page<EventClubServiceResponseDto> getEvents(Long providerId, Long clubId, Pageable pageable) {
		boolean existClub = clubServiceJpa.existsByIdAndUserId(clubId, providerId);
		if (!existClub) {
			throw new ValidationException(ErrorConstants.CLUB_NOT_FOUND_OR_NOT_AUTHORIZED.name(),
					ErrorConstants.CLUB_NOT_FOUND_OR_NOT_AUTHORIZED.getMessage());
		}

		Page<EventClubServiceEntity> events = clubServiceJpa.findByClubServiceId(clubId, pageable);

		List<EventClubServiceResponseDto> dtoList = new ArrayList<>();
		for (EventClubServiceEntity entity : events.getContent()) {
			EventClubServiceResponseDto dto = EventClubServiceResponseDto.builder().id(entity.getId())
					.clubServiceId(entity.getClubService().getId()).name(entity.getName())
					.startTime(entity.getStartTime()).endTime(entity.getEndTime()).images(entity.getImages()).build();

			dtoList.add(dto);
		}

		return new PageImpl<>(dtoList, pageable, events.getTotalElements());
	}

	@Override
	@Transactional(readOnly = true)
	public Page<EventClubServiceResponseDto> getEventsToday(Long providerId, Long clubId, Pageable pageable) {
		boolean existClub = clubServiceJpa.existsByIdAndUserId(clubId, providerId);
		if (!existClub) {
			throw new ValidationException(ErrorConstants.CLUB_NOT_FOUND_OR_NOT_AUTHORIZED.name(),
					ErrorConstants.CLUB_NOT_FOUND_OR_NOT_AUTHORIZED.getMessage());
		}

		ZoneId zoneId = ZoneId.systemDefault();
		LocalDate today = LocalDate.now(zoneId);
		;
		OffsetDateTime startOfDay = today.atStartOfDay().atOffset(ZoneOffset.UTC);
		OffsetDateTime endOfDay = today.plusDays(1).atStartOfDay().atOffset(ZoneOffset.UTC);

		Page<EventClubServiceEntity> events = clubServiceJpa.findTodayEventsByClubServiceId(clubId, startOfDay,
				endOfDay, pageable);

		List<EventClubServiceResponseDto> dtoList = new ArrayList<>();
		for (EventClubServiceEntity entity : events.getContent()) {
			EventClubServiceResponseDto dto = EventClubServiceResponseDto.builder().id(entity.getId())
					.name(entity.getName()).startTime(entity.getStartTime()).endTime(entity.getEndTime())
					.images(entity.getImages()).build();

			dtoList.add(dto);
		}

		return new PageImpl<>(dtoList, pageable, events.getTotalElements());
	}

	@Override
	@Transactional(readOnly = true)
	public Page<EventClubServiceResponseDto> getEventsForPublic(Long clubId, Pageable pageable) {
		Page<EventClubServiceEntity> events = clubServiceJpa.findByClubServiceId(clubId, pageable);

		List<EventClubServiceResponseDto> dtoList = new ArrayList<>();
		for (EventClubServiceEntity entity : events.getContent()) {
			dtoList.add(toEventClubServiceResponseDto(entity));
		}

		return new PageImpl<>(dtoList, pageable, events.getTotalElements());
	}

	// FRONTEND - lista club pubblici
	@Override
	@Transactional(readOnly = true)
	public List<ServiceSummaryResponseDto> getActiveClubs(String searchText) {
		List<ClubServiceEntity> entity = clubServiceJpa.findActiveBySearch(searchText);

		return entity.stream().map(clubServiceMapper::toSummaryDto).collect(Collectors.toList());
	}

	// FRONTEND - club con eventi attivi
	@Override
	@Transactional(readOnly = true)
	public List<ServiceSummaryResponseDto> getClubsWithEvents() {
		List<ClubServiceEntity> entities = clubServiceJpa.findActiveWithEvents();

		return entities.stream().map(clubServiceMapper::toSummaryDto).collect(Collectors.toList());
	}

	@Override
	@Transactional(readOnly = true)
	public ClubManagementResponseDto getProviderClub(Long providerId, Long clubId) {
		User user = userJpa.findById(providerId)
				.orElseThrow(() -> new ValidationException(ErrorConstants.USER_NOT_FOUND.name(),
						ErrorConstants.USER_NOT_FOUND.getMessage()));

		ClubServiceEntity entity = clubServiceJpa.findById(clubId)
				.orElseThrow(() -> new ValidationException(ErrorConstants.PROVIDER_NOT_ALLOWED.name(),
						ErrorConstants.PROVIDER_NOT_ALLOWED.getMessage()));

		// Conversione Entity -> DTO
		return ClubManagementResponseDto.builder().id(entity.getId()).name(entity.getName())
				.description(entity.getDescription())
				.locales(serviceLocaleMapper.mapEntityToReponse(entity.getLocales()))
				.maxCapacity(entity.getMaxCapacity()).build();
	}

	@Override
	@Transactional(readOnly = true)
	public EventClubServiceResponseDto getEvent(Long eventId) {
		EventClubServiceEntity event = eventClubServiceRepository.findById(eventId)
				.orElseThrow(() -> new ValidationException(ErrorConstants.EVENT_NOT_FOUND.name(),
						ErrorConstants.EVENT_NOT_FOUND.getMessage()));

		boolean available = true;
		if (!event.getIsActive() || event.getMaxCapacity() <= event.getOccupiedCapacity()
				|| !event.getEndTime().isAfter(OffsetDateTime.now())) {
			available = false;
		}

		return EventClubServiceResponseDto.builder().id(event.getId()).name(event.getName())
				.clubServiceId(event.getClubService().getId())
				.description(event.getDescription())
				.serviceLocale(serviceLocaleMapper.mapEntityToReponse(event.getLocales()))
				.startTime(event.getStartTime()).endTime(event.getEndTime()).theme(event.getTheme())
				.images(event.getImages()).available(available).price(event.getPrice()).djName(event.getDjName())
				.pricingConfigurations(eventPricingConfigurationMapper.toDtoList(event.getPricingConfigurations()))
				.maxCapacity(event.getMaxCapacity()).build();
	}

	@Override
	@Transactional(readOnly = true)
	public EventClubServiceResponseDto getEventForProvider(Long providerId, Long eventId) {
		EventClubServiceEntity event = eventClubServiceRepository.findById(eventId)
				.orElseThrow(() -> new ValidationException(ErrorConstants.EVENT_NOT_FOUND.name(),
						ErrorConstants.EVENT_NOT_FOUND.getMessage()));

		if (providerId != event.getClubService().getUser().getId()) {
			new ValidationException(ErrorConstants.PROVIDER_NOT_ALLOWED.name(),
					ErrorConstants.PROVIDER_NOT_ALLOWED.getMessage());
		}

		String language = LocaleContextHolder.getLocale().getLanguage();

		List<ServiceLocale> filtered = event.getLocales().stream()
				.filter(locale -> locale.getLanguage().equalsIgnoreCase(language)).collect(Collectors.toList());

		List<ServiceLocaleResponseDto> filteredLocales = serviceLocaleMapper.mapEntityToReponse(filtered);

		return EventClubServiceResponseDto.builder().id(event.getId()).name(event.getName())
				.clubServiceId(event.getClubService().getId())
				.description(event.getDescription()).serviceLocale(filteredLocales).startTime(event.getStartTime())
				.endTime(event.getEndTime()).theme(event.getTheme()).images(event.getImages())
				.available(event.getIsActive()).price(event.getPrice()).djName(event.getDjName())
				.pricingConfigurations(eventPricingConfigurationMapper.toDtoList(event.getPricingConfigurations()))
				.maxCapacity(event.getMaxCapacity()).genderPercentage(event.getGenderPercentage()).build();
	}

	@Override
	@Transactional(readOnly = true)
	public Page<ClubBookingResponseDto> getUserClubBookings(Long userId, Pageable pageable) {
		// Verifica che l'utente esista
		User user = userJpa.findById(userId)
				.orElseThrow(() -> new ValidationException(ErrorConstants.USER_NOT_FOUND.name(),
						ErrorConstants.USER_NOT_FOUND.getMessage()));

		Page<ClubBooking> bookings = clubBookingJpa.findByUserId(userId, pageable);
		return bookings.map(bookingMapper::toClubResponseDto);
	}

	public ClubServiceResponseDto getClubDetail(Long clubId) {
		ClubServiceEntity club = clubServiceJpa.findById(clubId)
				.orElseThrow(() -> new ValidationException(ErrorConstants.CLUB_NOT_FOUND.name(),
						ErrorConstants.CLUB_NOT_FOUND.getMessage()));

		return clubServiceMapper.toDetailDto(club);
	}

	// CLIENTE - crea prenotazione club
	@Override
	@Transactional
	public ClubBookingResponseDto createClubBooking(ClubBookingRequestDto requestDto, Long userId) {
		User user = userJpa.findById(userId)
				.orElseThrow(() -> new ValidationException(ErrorConstants.USER_NOT_FOUND.name(),
						ErrorConstants.USER_NOT_FOUND.getMessage()));

		ClubServiceEntity clubService = clubServiceJpa.findById(requestDto.getClubServiceId())
				.orElseThrow(() -> new ValidationException(ErrorConstants.CLUB_NOT_FOUND.name(),
						ErrorConstants.CLUB_NOT_FOUND.getMessage()));

		checkClubAvailability(clubService, requestDto.getReservationTime(), requestDto.getNumberOfPeople());

		ClubBooking booking = bookingMapper.toClubEntity(requestDto, user, clubService);
		ClubBooking savedBooking = clubBookingJpa.save(booking);

		return bookingMapper.toClubResponseDto(savedBooking);
	}

	// FORNITORE - crea/aggiorna club
	@Override
	@Transactional
	public ServiceDetailResponseDto createClub(Long providerId, ClubServiceRequestDto requestDto,
			List<MultipartFile> images) throws Exception {
		User provider = userJpa.findById(providerId)
				.orElseThrow(() -> new ValidationException(ErrorConstants.USER_NOT_FOUND.name(),
						ErrorConstants.USER_NOT_FOUND.getMessage()));

		Boolean providerIsAllowedForService = userServiceVerificationJpa
				.existsByUserIdAndServiceTypeAndVerificationStatus(provider.getId(), ServiceType.CLUB,
						VerificationStatusServiceEY.ACTIVE);
		if (!providerIsAllowedForService) {
			throw new ValidationException(ErrorConstants.PROVIDER_NOT_ALLOWED.name(),
					ErrorConstants.PROVIDER_NOT_ALLOWED.getMessage());
		}

		ClubServiceEntity club = clubServiceMapper.toEntity(requestDto, provider);
		club.setUser(provider);

		ClubServiceEntity savedClub = clubServiceJpa.save(club);

		savedClub.setImages(new ArrayList<String>());
		fileService.uploadImages(savedClub.getId(), ServiceType.CLUB, savedClub.getImages(), images);

		savedClub = clubServiceJpa.save(savedClub);

		return clubServiceMapper.toDetailDto(savedClub);
	}

	@Override
	@Transactional
	public ServiceDetailResponseDto updateClub(Long providerId, Long serviceId, ClubServiceRequestDto requestDto,
			List<MultipartFile> newImages) throws Exception {
		User provider = userJpa.findById(providerId)
				.orElseThrow(() -> new ValidationException(ErrorConstants.USER_NOT_FOUND.name(),
						ErrorConstants.USER_NOT_FOUND.getMessage()));

		ClubServiceEntity club = clubServiceJpa.findById(serviceId)
				.orElseThrow(() -> new ValidationException(ErrorConstants.SERVICE_NOT_FOUND.name(),
						ErrorConstants.SERVICE_NOT_FOUND.getMessage()));
		if (club.getUser().getId() != providerId) {
			throw new ValidationException(ErrorConstants.PROVIDER_NOT_ALLOWED.name(),
					ErrorConstants.PROVIDER_NOT_ALLOWED.getMessage());
		}

		club.setUser(provider);
		club.setName(requestDto.getName());
		club.setDescription(requestDto.getDescription());
		club.setMaxCapacity(requestDto.getMaxCapacity());
		club.setBasePrice(requestDto.getBasePrice());
		club.setPublicationStatus(requestDto.getPublicationStatus());

		List<ServiceLocale> updatedLocales = serviceLocaleMapper.mapRequestToEntity(requestDto.getLocales());
		club.getLocales().clear();
		club.getLocales().addAll(updatedLocales);

		ClubServiceEntity savedClub = clubServiceJpa.save(club);

		if (newImages != null && !newImages.isEmpty()) {
			fileService.updateImages(savedClub.getId(), ServiceType.CLUB, savedClub.getImages(), newImages);

			savedClub = clubServiceJpa.save(savedClub);
		}

		return clubServiceMapper.toDetailDto(savedClub);
	}

	@Override
	@Transactional
	public EventClubServiceResponseDto createEvent(Long providerId, Long serviceId,
			EventClubServiceRequestDto requestDto, List<MultipartFile> images) throws Exception {
		User provider = userJpa.findById(providerId)
				.orElseThrow(() -> new ValidationException(ErrorConstants.USER_NOT_FOUND.name(),
						ErrorConstants.USER_NOT_FOUND.getMessage()));

		Boolean providerIsAllowedForService = userServiceVerificationJpa
				.existsByUserIdAndServiceTypeAndVerificationStatus(provider.getId(), ServiceType.CLUB,
						VerificationStatusServiceEY.ACTIVE);
		if (!providerIsAllowedForService) {
			throw new ValidationException(ErrorConstants.PROVIDER_NOT_ALLOWED.name(),
					ErrorConstants.PROVIDER_NOT_ALLOWED.getMessage());
		}

		ClubServiceEntity club = clubServiceJpa.findById(serviceId)
				.orElseThrow(() -> new ValidationException(ErrorConstants.SERVICE_NOT_FOUND.name(),
						ErrorConstants.SERVICE_NOT_FOUND.getMessage()));

		List<EventClubServiceEntity> events = club.getEvents();
		EventClubServiceEntity event = EventClubServiceEntity.builder().clubService(club).name(requestDto.getName())
				.description(requestDto.getDescription())
				.locales(serviceLocaleMapper.mapRequestToEntity(requestDto.getServiceLocale()))
				.startTime(requestDto.getStartTime()).endTime(requestDto.getEndTime()).price(requestDto.getPrice())
				.maxCapacity(requestDto.getMaxCapacity()).djName(requestDto.getDjName()).theme(requestDto.getTheme())
				.isActive(requestDto.getIsActive()).creationDate(OffsetDateTime.now()).build();

		// Salva l'evento prima per ottenere l'ID
		events.add(event);
		club.setEvents(events);
		ClubServiceEntity savedClub = clubServiceJpa.save(club);

		// Recupero l'evento salvato con ID generato
		EventClubServiceEntity savedEvent = savedClub.getEvents().stream()
				.filter(e -> e.getName().equals(event.getName()) && e.getStartTime().equals(event.getStartTime()))
				.findFirst().orElseThrow(() -> new RuntimeException("Evento salvato non trovato"));

		// Salva le configurazioni di prezzo
		if (requestDto.getPricingConfigurations() != null && !requestDto.getPricingConfigurations().isEmpty()) {
			List<EventPricingConfiguration> pricingConfigurations = eventPricingConfigurationMapper
					.toEntityList(requestDto.getPricingConfigurations(), savedEvent);

			eventPricingConfigurationService.saveAll(pricingConfigurations);
		}

		List<String> imageNames = new ArrayList<>();
		Path basePath = Paths.get(ImageUtils.STORAGE_ROOT, ServiceType.CLUB.name().toLowerCase(),
				savedClub.getId().toString(), savedEvent.getId().toString());

		fileService.uploadImagesPathCustom(basePath, imageNames, images);

		savedEvent.setImages(imageNames);
		clubServiceJpa.save(savedClub);

		return EventClubServiceResponseDto.builder().id(savedEvent.getId()).name(savedEvent.getName())
				.clubServiceId(event.getClubService().getId())
				.description(savedEvent.getDescription())
				.serviceLocale(serviceLocaleMapper.mapEntityToReponse(savedEvent.getLocales()))
				.startTime(savedEvent.getStartTime()).endTime(savedEvent.getEndTime()).theme(savedEvent.getTheme())
				.available(savedEvent.getIsActive()).price(savedEvent.getPrice()).djName(savedEvent.getDjName())
				.pricingConfigurations(eventPricingConfigurationMapper.toDtoList(savedEvent.getPricingConfigurations()))
				.maxCapacity(savedEvent.getMaxCapacity()).build();
	}

	@Override
	@Transactional
	public EventClubServiceResponseDto createEvent(Long providerId, EventClubServiceRequestDto requestDto,
			List<MultipartFile> images) throws Exception {

		User provider = userJpa.findById(providerId)
				.orElseThrow(() -> new ValidationException(ErrorConstants.USER_NOT_FOUND.name(),
						ErrorConstants.USER_NOT_FOUND.getMessage()));

		Boolean providerIsAllowedForService = userServiceVerificationJpa
				.existsByUserIdAndServiceTypeAndVerificationStatus(provider.getId(), ServiceType.CLUB,
						VerificationStatusServiceEY.ACTIVE);
		if (!providerIsAllowedForService) {
			throw new ValidationException(ErrorConstants.PROVIDER_NOT_ALLOWED.name(),
					ErrorConstants.PROVIDER_NOT_ALLOWED.getMessage());
		}

		// Recupera il club del provider
		ClubServiceEntity club = clubServiceJpa.findByProviderId(providerId)
				.orElseThrow(() -> new ValidationException(ErrorConstants.CLUB_NOT_FOUND.name(),
						ErrorConstants.CLUB_NOT_FOUND.getMessage()));

		// Crea la nuova entità evento
		EventClubServiceEntity event = new EventClubServiceEntity();
		event.setClubService(club);

		event.setName(requestDto.getName());
		event.setDescription(requestDto.getDescription());

		// === GESTIONE SICURA DEI LOCALES ===
		List<ServiceLocale> locales = new ArrayList<>();
		List<ServiceLocale> newLocales = serviceLocaleMapper.mapRequestToEntity(requestDto.getServiceLocale());
		if (newLocales != null && !newLocales.isEmpty()) {
			for (ServiceLocale locale : newLocales) {
				locale.setServiceType(ServiceType.CLUB);
				if (locale.getLanguage() == null) {
					locale.setLanguage("it");
				}
			}
			locales.addAll(newLocales);
		}
		event.setLocales(locales);
		// === FINE ===

		event.setStartTime(requestDto.getStartTime());
		event.setEndTime(requestDto.getEndTime());
		event.setPrice(requestDto.getPrice());
		event.setMaxCapacity(requestDto.getMaxCapacity());
		event.setTheme(requestDto.getTheme());
		event.setIsActive(requestDto.getIsActive());
		event.setDressCode(requestDto.getDressCode());
		event.setGenderPercentage(requestDto.getGenderPercentage());
		event.setOccupiedCapacity(requestDto.getMaxCapacity());

		// Gestione pricing configurations
		if (requestDto.getPricingConfigurations() != null && !requestDto.getPricingConfigurations().isEmpty()) {
			List<EventPricingConfiguration> pricingConfigs = eventPricingConfigurationMapper
					.toEntityList(requestDto.getPricingConfigurations(), event);
			
			eventPricingConfigurationService.saveAll(pricingConfigs);
		}

		// === GESTIONE IMMAGINI CON FILESERVICE ===
		club.getEvents().add(event);

		// Primo salvataggio: necessario per generare l'ID dell'evento (grazie al
		// cascade sul club)
		clubServiceJpa.save(club);

		Long eventId = event.getId(); // Ora l'ID è generato

		Path basePath = Paths.get(ImageUtils.STORAGE_ROOT, ServiceType.CLUB.name().toLowerCase(),
				club.getId().toString(), "events", eventId.toString());

		List<String> eventImages = new ArrayList<>();

		// Carica le immagini usando il FileService (lista vecchia vuota → solo upload
		// nuove)
		fileService.updateImagesPathCustom(basePath, eventImages, images);

		event.setImages(eventImages);
		// === FINE GESTIONE IMMAGINI ===

		// Salvataggio finale tramite il club (come richiesto)
		clubServiceJpa.save(club);

		return EventClubServiceResponseDto.builder().id(event.getId()).name(event.getName())
				.clubServiceId(event.getClubService().getId())
				.description(event.getDescription())
				.serviceLocale(serviceLocaleMapper.mapEntityToReponse(event.getLocales()))
				.startTime(event.getStartTime()).endTime(event.getEndTime()).theme(event.getTheme())
				.available(event.getIsActive()).price(event.getPrice()).maxCapacity(event.getMaxCapacity())
				.occupiedCapacity(event.getOccupiedCapacity()).dressCode(event.getDressCode())
				.genderPercentage(event.getGenderPercentage()).images(event.getImages())
				.pricingConfigurations(eventPricingConfigurationMapper.toDtoList(event.getPricingConfigurations()))
				.build();
	}

	@Override
	@Transactional
	public EventClubServiceResponseDto updateEvent(Long providerId, Long eventId, EventClubServiceRequestDto requestDto,
			List<MultipartFile> newImages) throws Exception {

		User provider = userJpa.findById(providerId)
				.orElseThrow(() -> new ValidationException(ErrorConstants.USER_NOT_FOUND.name(),
						ErrorConstants.USER_NOT_FOUND.getMessage()));

		Boolean providerIsAllowedForService = userServiceVerificationJpa
				.existsByUserIdAndServiceTypeAndVerificationStatus(provider.getId(), ServiceType.CLUB,
						VerificationStatusServiceEY.ACTIVE);
		if (!providerIsAllowedForService) {
			throw new ValidationException(ErrorConstants.PROVIDER_NOT_ALLOWED.name(),
					ErrorConstants.PROVIDER_NOT_ALLOWED.getMessage());
		}

		// Recupera l'evento tramite join con club
		EventClubServiceEntity event = clubServiceJpa.findByEventsId(eventId)
				.orElseThrow(() -> new ValidationException(ErrorConstants.EVENT_NOT_FOUND.name(),
						ErrorConstants.EVENT_NOT_FOUND.getMessage()));

		ClubServiceEntity club = event.getClubService();
		if (!Objects.equals(club.getUser().getId(), providerId)) {
			throw new ValidationException(ErrorConstants.PROVIDER_NOT_ALLOWED.name(),
					ErrorConstants.PROVIDER_NOT_ALLOWED.getMessage());
		}

		// Aggiornamento campi base
		event.setName(requestDto.getName());
		event.setDescription(requestDto.getDescription());

		// === GESTIONE SICURA DEI LOCALES ===
		List<ServiceLocale> currentLocales = event.getLocales();
		if (currentLocales == null) {
			currentLocales = new ArrayList<>();
			event.setLocales(currentLocales);
		}
		currentLocales.clear(); // rimuove i vecchi
		List<ServiceLocale> newLocales = serviceLocaleMapper.mapRequestToEntity(requestDto.getServiceLocale());
		if (newLocales != null && !newLocales.isEmpty()) {
			for (ServiceLocale locale : newLocales) {
				locale.setServiceType(ServiceType.CLUB);
				if (locale.getLanguage() == null)
					locale.setLanguage("it");
			}
			currentLocales.addAll(newLocales);
		}
		// === FINE ===

		event.setStartTime(requestDto.getStartTime());
		event.setEndTime(requestDto.getEndTime());
		event.setPrice(requestDto.getPrice());
		event.setMaxCapacity(requestDto.getMaxCapacity());
		event.setTheme(requestDto.getTheme());
		event.setIsActive(requestDto.getIsActive());
		event.setDressCode(requestDto.getDressCode());
		event.setGenderPercentage(requestDto.getGenderPercentage());

		// Gestione pricing configurations
		if (requestDto.getPricingConfigurations() != null && !requestDto.getPricingConfigurations().isEmpty()) {
			eventPricingConfigurationService.deleteAll(event.getPricingConfigurations());
			List<EventPricingConfiguration> newPricing = eventPricingConfigurationMapper
					.toEntityList(requestDto.getPricingConfigurations(), event);
			eventPricingConfigurationService.saveAll(newPricing);
		} else {
			eventPricingConfigurationService.deleteAll(event.getPricingConfigurations());
		}

		// === GESTIONE IMMAGINI DELL'EVENTO CON FILESERVICE ===
		Path basePath = Paths.get(ImageUtils.STORAGE_ROOT, ServiceType.CLUB.name().toLowerCase(),
				club.getId().toString(), "event", eventId.toString());

		List<String> currentImages = event.getImages() != null ? 
				new ArrayList<>(event.getImages()) 
				: new ArrayList<>();

		// Usa il FileService per gestire la sostituzione totale delle immagini
		fileService.updateImagesPathCustom(basePath, currentImages, newImages);

		event.setImages(currentImages);
		// === FINE GESTIONE IMMAGINI ===

		clubServiceJpa.save(club);

		return EventClubServiceResponseDto.builder().id(event.getId()).name(event.getName())
				.description(event.getDescription())
				.serviceLocale(serviceLocaleMapper.mapEntityToReponse(event.getLocales()))
				.startTime(event.getStartTime()).endTime(event.getEndTime()).theme(event.getTheme())
				.available(event.getIsActive()).price(event.getPrice()).maxCapacity(event.getMaxCapacity())
				.occupiedCapacity(event.getOccupiedCapacity()).dressCode(event.getDressCode())
				.genderPercentage(event.getGenderPercentage()).images(event.getImages())
				.pricingConfigurations(eventPricingConfigurationMapper.toDtoList(event.getPricingConfigurations()))
				.build();
	}

	private void checkClubAvailability(ClubServiceEntity club, OffsetDateTime reservationTime, Integer numberOfPeople) {
		if (numberOfPeople > club.getMaxCapacity()) {
			throw new ValidationException(ErrorConstants.LIMIT_MAX_PEOPLE_CLUB.name(),
					ErrorConstants.LIMIT_MAX_PEOPLE_CLUB.getMessage());
		}
	}

	@Override
	@Transactional(readOnly = true)
	public Page<EventPublicResponseDto> getEventsForPublicWithFilters(String location, String eventType, String date,
			Boolean table, Pageable pageable) {
		return clubEventJdbc.getEventsForPublicWithFilters(location, eventType, date, table, pageable);
	}

	private EventClubServiceResponseDto toEventClubServiceResponseDto(EventClubServiceEntity entity) {
		// Costruzione lista delle configurazioni di prezzo
		List<EventPricingConfigurationResponseDto> priceDtos = new ArrayList<>();
		if (entity.getPricingConfigurations() != null && !entity.getPricingConfigurations().isEmpty()) {
			for (EventPricingConfiguration pc : entity.getPricingConfigurations()) {
				EventPricingConfigurationResponseDto priceDto = EventPricingConfigurationResponseDto.builder()
						.id(pc.getId()).price(pc.getPrice()).description(pc.getDescription()).build();
				priceDtos.add(priceDto);
			}
		}

		// Costruzione DTO principale con builder
		return EventClubServiceResponseDto.builder().id(entity.getId())
				.clubServiceId(entity.getClubService() != null ? entity.getClubService().getId() : null)
				.name(entity.getName()).description(entity.getDescription()).startTime(entity.getStartTime())
				.endTime(entity.getEndTime()).price(entity.getPrice()).maxCapacity(entity.getMaxCapacity())
				.djName(entity.getDjName()).theme(entity.getTheme()).images(entity.getImages())
				.available(entity.getIsActive())
				.serviceLocale(serviceLocaleMapper.mapEntityToReponse(entity.getLocales()))
				.pricingConfigurations(priceDtos.isEmpty() ? null : priceDtos).build();
	}

	private us.hogu.controller.dto.response.EventPublicResponseDto toEventPublicResponseDto(
			EventClubServiceEntity entity) {
		// Extract pricing information from EventPricingConfiguration
		java.math.BigDecimal priceMan = null;
		java.math.BigDecimal priceWoman = null;
		java.math.BigDecimal priceMinSpend = null;

		if (entity.getPricingConfigurations() != null && !entity.getPricingConfigurations().isEmpty()) {
			for (EventPricingConfiguration pc : entity.getPricingConfigurations()) {
				if (pc.getPricingType() == us.hogu.model.enums.PricingType.MALE) {
					priceMan = pc.getPrice();
				} else if (pc.getPricingType() == us.hogu.model.enums.PricingType.FEMALE) {
					priceWoman = pc.getPrice();
				} else if (pc.getPricingType() == us.hogu.model.enums.PricingType.VIP_TABLE
						|| pc.getPricingType() == us.hogu.model.enums.PricingType.STANDARD_TABLE) {
					if (priceMinSpend == null
							|| pc.getPrice().compareTo(priceMinSpend) < 0) {
						priceMinSpend = pc.getPrice();
					}
				}
			}
		}

		// Get location from club service name (can be enhanced with proper location
		// field)
		String location = entity.getClubService() != null ? entity.getClubService().getName()
				: "Location Not Available";

		return us.hogu.controller.dto.response.EventPublicResponseDto.builder().id(entity.getId())
				.name(entity.getName()).location(location).description(entity.getDescription()).priceMan(priceMan)
				.priceWoman(priceWoman)
				// .priceMinSpend(priceMinSpend)
				.startTime(entity.getStartTime()).eventType(entity.getTheme()).build();
	}

}
