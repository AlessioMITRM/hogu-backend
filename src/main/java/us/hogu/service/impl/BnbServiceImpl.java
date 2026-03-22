package us.hogu.service.impl;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.validation.Valid;

import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import lombok.RequiredArgsConstructor;
import us.hogu.common.constants.ErrorConstants;
import us.hogu.common.util.ImageUtils;
import us.hogu.configuration.security.dto.UserAccount;
import us.hogu.controller.dto.request.BnbBookingRequestDto;
import us.hogu.controller.dto.request.BnbRoomPriceRequestDto;
import us.hogu.controller.dto.request.BnbRoomRequestDto;
import us.hogu.controller.dto.request.BnbSearchRequestDto;
import us.hogu.controller.dto.request.BnbServiceRequestDto;
import us.hogu.controller.dto.response.BnbBookingResponseDto;
import us.hogu.controller.dto.response.BnbRoomResponseDto;
import us.hogu.controller.dto.response.BnbSearchResponseDto;
import us.hogu.controller.dto.response.BnbSearchResponseDto.BnbSearchResultDto;
import us.hogu.controller.dto.response.BnbServiceDetailResponseDto;
import us.hogu.controller.dto.response.BnbServiceResponseDto;
import us.hogu.controller.dto.response.InfoStatsDto;
import us.hogu.converter.BnbMapper;
import us.hogu.converter.ServiceLocaleMapper;
import us.hogu.exception.ResourceNotFoundException;
import us.hogu.exception.UserNotFoundException;
import us.hogu.exception.ValidationException;
import us.hogu.model.BnbBooking;
import us.hogu.model.BnbRoom;
import us.hogu.model.BnbRoomPriceCalendar;
import us.hogu.model.BnbServiceEntity;
import us.hogu.model.User;
import us.hogu.model.enums.BookingStatus;
import us.hogu.model.enums.ServiceType;
import us.hogu.repository.jdbc.BnbRoomJdbc;
import us.hogu.repository.jpa.BnbBookingJpa;
import us.hogu.repository.jpa.BnbRoomJpa;
import us.hogu.repository.jpa.BnbRoomPriceCalendarJpa;
import us.hogu.repository.jpa.BnbServiceJpa;
import us.hogu.repository.jpa.UserJpa;
import us.hogu.service.intefaces.BnbService;
import us.hogu.service.intefaces.FileService;

import us.hogu.service.redis.RedisAvailabilityService;
import us.hogu.service.mq.BookingProducer;
import us.hogu.controller.dto.request.BnbBookingEvent;

@RequiredArgsConstructor
@Service
public class BnbServiceImpl implements BnbService {
	private final UserJpa userJpa;
	private final BnbServiceJpa bnbServiceJpa;
	private final BnbRoomJpa bnbRoomJpa;
	private final BnbRoomPriceCalendarJpa bnbRoomPriceCalendarJpa;
	private final BnbBookingJpa bnbBookingJpa;
	private final BnbRoomJdbc bnbRoomJdbc;
	private final FileService fileService;
	private final ServiceLocaleMapper localeMapper;
	private final BnbMapper bnbMapper;
	private final RedisAvailabilityService redisService;
	private final BookingProducer bookingProducer;

	// 🔹 RICERCA CAMERE B&B
	@Override
	@Transactional(readOnly = true)
	public InfoStatsDto getInfo(Long providerId) {
		BnbServiceEntity entity = bnbServiceJpa.findByProviderIdForSingleService(providerId)
				.orElseThrow(() -> new ValidationException(
						ErrorConstants.SERVICE_BNB_NOT_FOUND.name(),
						ErrorConstants.SERVICE_BNB_NOT_FOUND.getMessage()));

		InfoStatsDto infoStats = bnbServiceJpa.getInfoStatsByProviderId(providerId);
		infoStats.setServiceId(entity.getId());

		return infoStats;
	}

	@Override
	@Transactional(readOnly = true)
	public BnbSearchResponseDto searchBnbRooms(BnbSearchRequestDto searchRequest) {

		// Validazione date
		if (searchRequest.getCheckIn() != null && searchRequest.getCheckOut() != null
				&& !searchRequest.isValidDateRange()) {
			throw new IllegalArgumentException("Check-out date must be after check-in date");
		}

		Pageable pageable = PageRequest.of(searchRequest.getPage(), searchRequest.getSize());

		String language = LocaleContextHolder.getLocale().getLanguage();
		if (searchRequest.getLocale() != null && searchRequest.getLocale().getLanguage() != null
				&& !searchRequest.getLocale().getLanguage().isEmpty()) {
			language = searchRequest.getLocale().getLanguage();
		}

		Page<BnbSearchResultDto> response = bnbRoomJdbc.searchNative(searchRequest,
				pageable, language);

		List<BnbSearchResultDto> content = new ArrayList<>(response.getContent());
		for (BnbSearchResultDto result : content) {
			result.setAvailable(true);
		}

		return BnbSearchResponseDto.builder()
				.content(content)
				.totalPages(response.getTotalPages())
				.totalElements(response.getTotalElements())
				.currentPage(response.getNumber())
				.pageSize(response.getSize())
				.build();
	}

	// 🔹 OTTIENI DETTAGLI SERVIZIO PER EDIT (SOLO PROPRIETARIO)
	@Override
	@Transactional(readOnly = true)
	public BnbServiceDetailResponseDto getBnbServiceByIdAndProvider(Long serviceId, Long providerId) {
		BnbServiceEntity entity = bnbServiceJpa.findByIdAndUserId(serviceId, providerId)
				.orElseThrow(() -> new ValidationException(
						ErrorConstants.SERVICE_BNB_NOT_FOUND.name(),
						ErrorConstants.SERVICE_BNB_NOT_FOUND.getMessage()));

		return BnbServiceDetailResponseDto.builder()
				.id(entity.getId())
				.name(entity.getName())
				.description(entity.getDescription())
				.defaultPricePerNight(entity.getDefaultPricePerNight())
				.totalRooms(entity.getTotalRooms())
				.maxGuestsForRoom(entity.getMaxGuestsForRoom())
				.locales(localeMapper.mapEntityToReponse(entity.getLocales()))
				.images(entity.getImages())
				.publicationStatus(entity.getPublicationStatus())
				.providerId(entity.getUser() != null ? entity.getUser().getId() : null)
				.build();
	}

	// 🔹 OTTIENI TUTTI I SERVIZI PUBBLICATI
	@Override
	public List<BnbServiceResponseDto> getAllPublishedBnbServices() {
		return bnbServiceJpa.findByPublicationStatus(true)
				.stream()
				.map(bnbMapper::toResponse)
				.collect(Collectors.toList());
	}

	// 🔹 OTTIENI UN SINGOLO SERVIZIO
	@Override
	public Optional<BnbServiceResponseDto> getBnbServiceById(Long id) {
		return bnbServiceJpa.findById(id).map(bnbMapper::toResponse);
	}

	// 🔹 OTTIENI LE CAMERE DI UN SERVIZIO
	@Override
	public Page<BnbRoomResponseDto> getRoomsForService(Long bnbServiceId, Pageable pageable) {
		Page<BnbRoom> roomsPage = bnbRoomJpa.findByBnbServiceId(bnbServiceId, pageable);
		List<BnbRoomResponseDto> content = bnbMapper.toRoomResponseList(roomsPage.getContent());
		return new org.springframework.data.domain.PageImpl<>(content, pageable, roomsPage.getTotalElements());
	}

	@Override
	public Page<BnbRoomResponseDto> getRoomsForServiceByProvider(Long providerId, Pageable pageable) {
		BnbServiceEntity service = bnbServiceJpa.findByProviderIdForSingleService(providerId)
				.orElseThrow(() -> new ValidationException(
						ErrorConstants.SERVICE_BNB_NOT_FOUND.name(),
						ErrorConstants.SERVICE_BNB_NOT_FOUND.getMessage()));

		Page<BnbRoom> roomsPage = bnbRoomJpa.findByBnbServiceId(service.getId(), pageable);
		List<BnbRoomResponseDto> content = bnbMapper.toRoomResponseList(roomsPage.getContent());
		return new org.springframework.data.domain.PageImpl<>(content, pageable, roomsPage.getTotalElements());
	}

	// 🔹 OTTIENI UNA CAMERE DI UN SERVIZIO
	@Override
	public BnbRoomResponseDto getRoomById(Long id, LocalDate checkIn, LocalDate checkOut) {
		Locale locale = LocaleContextHolder.getLocale();

		return bnbRoomJdbc.getRoomById(id, checkIn, checkOut, locale.getLanguage());
	}

	@Override
	public BnbRoomResponseDto getRoomByIdForProvider(Long id, Long providerId) {
		BnbRoom room = bnbRoomJpa.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("Camera non trovata"));

		BnbServiceEntity service = room.getBnbService();
		if (service == null || service.getUser() == null || !service.getUser().getId().equals(providerId)) {
			throw new ValidationException(
					ErrorConstants.UNAUTHORIZED.name(),
					"Non sei autorizzato a visualizzare questa camera.");
		}

		return bnbMapper.toRoomResponse(room);
	}

	// 🔹 LISTA PRENOTAZIONI UTENTE
	@Override
	public Page<BnbBookingResponseDto> getBookingsForUser(Long userId, Pageable pageable) {
		/*
		 * return bnbBookingJpa.findByUserId(userId)
		 * .stream()
		 * .map(bnbMapper::toBookingResponse)
		 * .collect(Collectors.toList());
		 */
		return null;
	}

	// 🔹 AGGIUNGI CAMERA A SERVIZIO

	// @Override
	@Transactional
	public BnbServiceResponseDto createBnbService(BnbServiceRequestDto dto, Long providerId) {
		User provider = userJpa.findById(providerId)
				.orElseThrow(() -> new UserNotFoundException("Fornitore non trovato"));

		BnbServiceEntity entity = bnbMapper.toEntity(dto);
		entity.setUser(provider);
		entity.setPublicationStatus(false);

		return bnbMapper.toResponse(bnbServiceJpa.save(entity));
	}

	// 🔹 AGGIUNGI PERIODO DI PREZZO AD UNA CAMERA
	// @Override
	@Transactional
	public void addRoomPrice(Long roomId, BnbRoomPriceRequestDto dto) {
		BnbRoom room = bnbRoomJpa.findById(roomId)
				.orElseThrow(() -> new ResourceNotFoundException("Camera non trovata"));

		BnbRoomPriceCalendar price = bnbMapper.toRoomPriceEntity(dto);
		price.setRoom(room);
		bnbRoomPriceCalendarJpa.save(price);
	}

	// 🔹 PRENOTAZIONE
	@Override
	@Transactional
	public BnbBookingResponseDto createBooking(BnbBookingRequestDto requestDto, Long userId) {
		BnbRoom room = bnbRoomJpa.findById(requestDto.getRoomId())
				.orElseThrow(() -> new ResourceNotFoundException("Camera non trovata"));
		BnbServiceEntity service = room.getBnbService();

		LocalDate checkIn = requestDto.getCheckInDate();
		LocalDate checkOut = requestDto.getCheckOutDate();
		Integer guests = requestDto.getNumberOfGuests();

		// 1. Redis Atomic Reservation
		List<LocalDate> reservedDates = new ArrayList<>();
		boolean success = true;
		LocalDate current = checkIn;
		int maxCapacity = room.getMaxGuests();

		while (current.isBefore(checkOut)) {
			// Lazy init with maxCapacity if key missing
			boolean reserved = redisService.reserve(room.getId(), current, guests, maxCapacity);
			if (reserved) {
				reservedDates.add(current);
			} else {
				success = false;
				break;
			}
			current = current.plusDays(1);
		}

		if (!success) {
			// Rollback acquired locks
			for (LocalDate date : reservedDates) {
				redisService.rollback(room.getId(), date, guests);
			}
			throw new ValidationException(ErrorConstants.GENERIC_ERROR.name(),
					"Disponibilità esaurita per le date selezionate (conflitto rilevato)");
		}

		// OPTIMIZATION: Load user only after successful reservation
		User user = userJpa.findById(userId)
				.orElseThrow(() -> new UserNotFoundException("Utente non trovato"));

		BigDecimal totalAmount = calculateTotalAmount(room, checkIn, checkOut);

		// 2. Async Event (Architecture Requirement)
		BnbBookingEvent event = BnbBookingEvent.builder()
				.userId(userId)
				.roomId(room.getId())
				.checkIn(checkIn)
				.checkOut(checkOut)
				.guests(guests)
				.totalAmount(totalAmount)
				.serviceId(service.getId())
				.build();

		try {
			bookingProducer.sendBookingRequest(event);
		} catch (Exception e) {
			// Log error but proceed since we have the Redis lock and will save to DB below
		}

		// 3. Sync Persistence (Source of Truth)
		// This ensures the DB is updated so Search queries (BnbRoomJdbc) remain
		// consistent.
		BnbBooking booking = BnbBooking.builder()
				.user(user)
				.bnbService(service)
				.room(room)
				.checkInDate(checkIn)
				.checkOutDate(checkOut)
				.numberOfGuests(guests)
				.status(us.hogu.model.enums.BookingStatus.PENDING)
				.totalAmount(totalAmount)
				.billingFirstName(requestDto.getBillingFirstName())
				.billingLastName(requestDto.getBillingLastName())
				.billingTaxCode(requestDto.getFiscalCode())
				.billingVatNumber(requestDto.getTaxId())
				.billingAddress(requestDto.getBillingAddress())
				.billingEmail(requestDto.getBillingEmail())
				.build();

		try {
			return bnbMapper.toBookingResponse(bnbBookingJpa.save(booking));
		} catch (Exception e) {
			// Rollback Redis on DB failure
			LocalDate rollbackCurrent = checkIn;
			while (rollbackCurrent.isBefore(checkOut)) {
				redisService.rollback(room.getId(), rollbackCurrent, guests);
				rollbackCurrent = rollbackCurrent.plusDays(1);
			}
			throw e;
		}
	}

	// 🔹 PRENOTAZIONE (LEGACY - DA RIMUOVERE O DEPRECARE)
	@Override
	@Transactional
	public BnbBookingResponseDto createBooking(Long userId, Long roomId, LocalDate checkIn, LocalDate checkOut,
			Integer guests) {
		BnbBookingRequestDto dto = new BnbBookingRequestDto();
		dto.setRoomId(roomId);
		dto.setCheckInDate(checkIn);
		dto.setCheckOutDate(checkOut);
		dto.setNumberOfGuests(guests);
		return createBooking(dto, userId);
	}

	@Override
	@Transactional
	public void addRoomPrice(UserAccount userAccount, Long roomId, BnbRoomPriceRequestDto dto) {
		BnbRoom room = bnbRoomJpa.findById(roomId)
				.orElseThrow(() -> new ResourceNotFoundException("Camera non trovata"));

		BnbServiceEntity service = room.getBnbService();
		if (service == null || service.getUser() == null
				|| !service.getUser().getId().equals(userAccount.getAccountId())) {
			throw new ValidationException(
					ErrorConstants.UNAUTHORIZED.name(),
					"Non sei autorizzato a modificare il listino prezzi di questa camera.");
		}

		BnbRoomPriceCalendar price = bnbMapper.toRoomPriceEntity(dto);
		price.setRoom(room);
		bnbRoomPriceCalendarJpa.save(price);
	}

	@Override
	@Transactional
	public BnbRoomResponseDto addRoomToService(UserAccount userAccount, Long providerId, BnbRoomRequestDto dto,
			List<MultipartFile> images) throws Exception {
		BnbServiceEntity service = bnbServiceJpa.findByProviderIdForSingleService(providerId)
				.orElseThrow(() -> new ValidationException(
						ErrorConstants.SERVICE_BNB_NOT_FOUND.name(),
						ErrorConstants.SERVICE_BNB_NOT_FOUND.getMessage()));

		if (!service.getUser().getId().equals(userAccount.getAccountId())) {
			throw new ValidationException(
					ErrorConstants.UNAUTHORIZED.name(),
					"Non sei autorizzato ad aggiungere camere a questo B&B.");
		}

		BnbRoom room = bnbMapper.toRoomEntity(dto);
		room.setBnbService(service);
		room.setPublicationStatus(dto.getEffectivePublicationStatus() != null ? dto.getEffectivePublicationStatus() : true);

		if (dto.getPriceCalendar() != null && !dto.getPriceCalendar().isEmpty()) {
			List<BnbRoomPriceCalendar> calendarEntries = new ArrayList<>();
			for (BnbRoomPriceRequestDto priceDto : dto.getPriceCalendar()) {
				BnbRoomPriceCalendar entry = bnbMapper.toRoomPriceEntity(priceDto);
				entry.setRoom(room);
				calendarEntries.add(entry);
			}
			room.setPriceCalendar(calendarEntries);
		}

		if (room.getImages() == null) {
			room.setImages(new ArrayList<>());
		}

		BnbRoom savedRoom = bnbRoomJpa.save(room);

		Path basePath = Paths.get(ImageUtils.STORAGE_ROOT,
				ServiceType.BNB.name().toLowerCase(),
				service.getId().toString(),
				savedRoom.getId().toString());

		fileService.uploadImagesPathCustom(basePath, savedRoom.getImages(), images);

		savedRoom = bnbRoomJpa.save(savedRoom);

		return bnbMapper.toRoomResponse(savedRoom);
	}

	@Override
	public Page<BnbBookingResponseDto> getBookingsForProvider(UserAccount userAccount, Long id, Pageable pageable) {
		BnbServiceEntity service = bnbServiceJpa.findByIdAndUserId(id, userAccount.getAccountId())
				.orElseThrow(() -> new ValidationException(
						ErrorConstants.SERVICE_BNB_NOT_FOUND.name(),
						ErrorConstants.SERVICE_BNB_NOT_FOUND.getMessage()));

		List<BookingStatus> requestStatuses = List.of(
				BookingStatus.PAYMENT_AUTHORIZED,
				BookingStatus.WAITING_PROVIDER_CONFIRMATION,
				BookingStatus.WAITING_CUSTOMER_PAYMENT);

		Sort sort = pageable.getSort().isUnsorted()
				? Sort.by(Sort.Direction.ASC, "creationDate")
				: pageable.getSort();

		Pageable sortedPageable = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), sort);

		Page<BnbBooking> bookingsPage = bnbBookingJpa.findByBnbServiceIdAndStatusIn(service.getId(), requestStatuses,
				sortedPageable);

		List<BnbBookingResponseDto> content = bnbMapper.toBookingResponseList(bookingsPage.getContent());
		return new org.springframework.data.domain.PageImpl<>(content, sortedPageable, bookingsPage.getTotalElements());
	}

	@Override
	@Transactional(readOnly = true)
	public Page<BnbBookingResponseDto> getTodayBookingsForProvider(UserAccount userAccount, Long id,
			Pageable pageable) {
		BnbServiceEntity service = bnbServiceJpa.findByIdAndUserId(id, userAccount.getAccountId())
				.orElseThrow(() -> new ValidationException(
						ErrorConstants.SERVICE_BNB_NOT_FOUND.name(),
						ErrorConstants.SERVICE_BNB_NOT_FOUND.getMessage()));

		LocalDate today = LocalDate.now();

		Sort sort = pageable.getSort().isUnsorted()
				? Sort.by(Sort.Direction.DESC, "creationDate")
				: pageable.getSort();

		Pageable sortedPageable = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), sort);

		Page<BnbBooking> bookingsPage = bnbBookingJpa.findByBnbServiceIdAndCheckInDate(service.getId(), today,
				sortedPageable);

		List<BnbBookingResponseDto> content = bnbMapper.toBookingResponseList(bookingsPage.getContent());
		return new org.springframework.data.domain.PageImpl<>(content, sortedPageable, bookingsPage.getTotalElements());
	}

	@Override
	@Transactional(readOnly = true)
	public Page<BnbBookingResponseDto> getUpcomingBookingsForProvider(UserAccount userAccount, Long id,
			Pageable pageable) {
		BnbServiceEntity service = bnbServiceJpa.findByIdAndUserId(id, userAccount.getAccountId())
				.orElseThrow(() -> new ValidationException(
						ErrorConstants.SERVICE_BNB_NOT_FOUND.name(),
						ErrorConstants.SERVICE_BNB_NOT_FOUND.getMessage()));

		LocalDate today = LocalDate.now();

		Sort sort = pageable.getSort().isUnsorted()
				? Sort.by(Sort.Order.asc("checkInDate"), Sort.Order.asc("creationDate"))
				: pageable.getSort();

		Pageable sortedPageable = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), sort);

		Page<BnbBooking> bookingsPage = bnbBookingJpa.findByBnbServiceIdAndCheckInDateGreaterThanEqual(
				service.getId(), today, sortedPageable);

		List<BnbBookingResponseDto> content = bnbMapper.toBookingResponseList(bookingsPage.getContent());
		return new org.springframework.data.domain.PageImpl<>(content, sortedPageable, bookingsPage.getTotalElements());
	}

	@Override
	@Transactional(readOnly = true)
	public Page<BnbBookingResponseDto> getHistoryBookingsForProvider(UserAccount userAccount, Long id,
			Pageable pageable) {
		BnbServiceEntity service = bnbServiceJpa.findByIdAndUserId(id, userAccount.getAccountId())
				.orElseThrow(() -> new ValidationException(
						ErrorConstants.SERVICE_BNB_NOT_FOUND.name(),
						ErrorConstants.SERVICE_BNB_NOT_FOUND.getMessage()));

		LocalDate today = LocalDate.now();

		Sort sort = pageable.getSort().isUnsorted()
				? Sort.by(Sort.Order.desc("checkInDate"), Sort.Order.desc("creationDate"))
				: pageable.getSort();

		Pageable sortedPageable = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), sort);

		Page<BnbBooking> bookingsPage = bnbBookingJpa.findByBnbServiceIdAndCheckInDateLessThan(
				service.getId(), today, sortedPageable);

		List<BnbBookingResponseDto> content = bnbMapper.toBookingResponseList(bookingsPage.getContent());
		return new org.springframework.data.domain.PageImpl<>(content, sortedPageable, bookingsPage.getTotalElements());
	}

	@Override
	@Transactional
	public BnbServiceResponseDto createBnbService(UserAccount userAccount, @Valid BnbServiceRequestDto request,
			List<MultipartFile> images) throws IOException {
		User provider = userJpa.findById(userAccount.getAccountId())
				.orElseThrow(() -> new UserNotFoundException("Fornitore non trovato"));

		BnbServiceEntity entity = bnbMapper.toEntity(request);
		entity.setUser(provider);
		if (request.getPublicationStatus() != null) {
			entity.setPublicationStatus(request.getPublicationStatus());
		} else {
			entity.setPublicationStatus(false);
		}

		if (request.getLocales() != null) {
			entity.setLocales(localeMapper.mapRequestToEntity(request.getLocales()));
		}

		if (request.getImages() == null) {
			entity.setImages(new ArrayList<>());
		} else {
			entity.setImages(new ArrayList<>(request.getImages()));
		}

		BnbServiceEntity saved = bnbServiceJpa.save(entity);

		Path basePath = Paths.get(ImageUtils.STORAGE_ROOT,
				ServiceType.BNB.name().toLowerCase(),
				saved.getId().toString());

		fileService.uploadImagesPathCustom(basePath, saved.getImages(), images);

		saved = bnbServiceJpa.save(saved);

		return bnbMapper.toResponse(saved);
	}

	@Override
	@Transactional
	public BnbServiceDetailResponseDto updateBnbService(Long id, UserAccount userAccount,
			@Valid BnbServiceRequestDto request,
			List<MultipartFile> images) throws Exception {
		BnbServiceEntity entity = bnbServiceJpa.findById(id)
				.orElseThrow(() -> new ValidationException(
						ErrorConstants.SERVICE_BNB_NOT_FOUND.name(),
						ErrorConstants.SERVICE_BNB_NOT_FOUND.getMessage()));

		if (!entity.getUser().getId().equals(userAccount.getAccountId())) {
			throw new ValidationException(
					ErrorConstants.UNAUTHORIZED.name(),
					"Non sei autorizzato a modificare questo B&B.");
		}

		// Update fields (check for nulls to allow partial updates and avoid DB
		// constraints errors)
		if (request.getName() != null)
			entity.setName(request.getName());
		if (request.getDescription() != null)
			entity.setDescription(request.getDescription());
		if (request.getDefaultPricePerNight() != null)
			entity.setDefaultPricePerNight(request.getDefaultPricePerNight());
		if (request.getTotalRooms() != null)
			entity.setTotalRooms(request.getTotalRooms());
		if (request.getMaxGuestsForRoom() != null)
			entity.setMaxGuestsForRoom(request.getMaxGuestsForRoom());
		if (request.getPublicationStatus() != null)
			entity.setPublicationStatus(request.getPublicationStatus());

		if (request.getLocales() != null) {
			entity.setLocales(localeMapper.mapRequestToEntity(request.getLocales()));
		}

		if (request.getImages() != null) {
			entity.setImages(request.getImages());
		}

		BnbServiceEntity updated = bnbServiceJpa.save(entity);

		Path basePath = Paths.get(ImageUtils.STORAGE_ROOT,
				ServiceType.BNB.name().toLowerCase(),
				updated.getId().toString());

		fileService.updateImagesPathCustom(basePath, updated.getImages(), images);

		updated = bnbServiceJpa.save(updated);

		return getBnbServiceByIdAndProvider(updated.getId(), userAccount.getAccountId());
	}

	@Override
	@Transactional(readOnly = true)
	public us.hogu.controller.dto.response.BnbBookingValidationResponseDto validateBnbBookingByCode(Long providerId,
			String bookingCode) {
		if (bookingCode == null) {
			return us.hogu.controller.dto.response.BnbBookingValidationResponseDto.builder().valid(false).build();
		}

		BnbBooking booking = bnbBookingJpa.findByBookingCode(bookingCode).orElse(null);
		if (booking == null) {
			return us.hogu.controller.dto.response.BnbBookingValidationResponseDto.builder()
					.valid(false)
					.build();
		}

		if (booking.getBnbService() == null
				|| booking.getBnbService().getUser() == null
				|| !booking.getBnbService().getUser().getId().equals(providerId)) {
			return us.hogu.controller.dto.response.BnbBookingValidationResponseDto.builder()
					.valid(false)
					.bookingId(booking.getId())
					.build();
		}

		boolean statusOk = booking.getStatus() == BookingStatus.FULL_PAYMENT_COMPLETED;

		LocalDate today = LocalDate.now();
		boolean isCheckInOrDuringStay = !today.isBefore(booking.getCheckInDate())
				&& !today.isAfter(booking.getCheckOutDate());

		boolean valid = statusOk && isCheckInOrDuringStay;

		String firstName = booking.getBillingFirstName() != null ? booking.getBillingFirstName()
				: (booking.getUser() != null ? booking.getUser().getName() : null);
		String lastName = booking.getBillingLastName() != null ? booking.getBillingLastName()
				: (booking.getUser() != null ? booking.getUser().getSurname() : null);
		String fullName = ((firstName != null ? firstName : "") + " " + (lastName != null ? lastName : "")).trim();

		return us.hogu.controller.dto.response.BnbBookingValidationResponseDto.builder()
				.valid(valid)
				.bookingId(booking.getId())
				.bnbServiceId(booking.getBnbService().getId())
				.roomName(booking.getRoom() != null ? booking.getRoom().getName() : null)
				.fullName(fullName)
				.checkInDate(booking.getCheckInDate() != null ? booking.getCheckInDate().toString() : null)
				.checkOutDate(booking.getCheckOutDate() != null ? booking.getCheckOutDate().toString() : null)
				.guests(booking.getNumberOfGuests())
				.build();
	}

	@Override
	@Transactional
	public Object updateRoom(UserAccount userAccount, Long providerId, Long roomId, @Valid BnbRoomRequestDto request,
			List<MultipartFile> images) throws Exception {
		BnbServiceEntity service = bnbServiceJpa.findByProviderIdForSingleService(userAccount.getAccountId())
				.orElseThrow(() -> new ValidationException(
						ErrorConstants.SERVICE_BNB_NOT_FOUND.name(),
						ErrorConstants.SERVICE_BNB_NOT_FOUND.getMessage()));

		if (!service.getUser().getId().equals(userAccount.getAccountId())) {
			throw new ValidationException(
					ErrorConstants.UNAUTHORIZED.name(),
					"Non sei autorizzato a modificare questo B&B.");
		}

		BnbRoom room = bnbRoomJpa.findById(roomId)
				.orElseThrow(() -> new ResourceNotFoundException("Camera non trovata"));

		if (room.getBnbService() == null || !room.getBnbService().getId().equals(service.getId())) {
			throw new ValidationException(
					ErrorConstants.UNAUTHORIZED.name(),
					"La camera non appartiene al servizio specificato.");
		}

		if (request.getName() != null) {
			room.setName(request.getName());
		}
		if (request.getDescription() != null) {
			room.setDescription(request.getDescription());
		}

		Integer oldCapacity = room.getMaxGuests();
		if (request.getMaxGuests() != null) {
			room.setMaxGuests(request.getMaxGuests());
		}
		Integer newCapacity = room.getMaxGuests();

		if (oldCapacity != null && newCapacity != null) {
			redisService.updateBnbRoomCapacity(roomId, oldCapacity, newCapacity);
		}

		if (request.getPriceForNight() != null) {
			room.setBasePricePerNight(request.getPriceForNight());
		}
		if (request.getEffectivePublicationStatus() != null) {
			room.setPublicationStatus(request.getEffectivePublicationStatus());
		}

		if (request.getPriceCalendar() != null) {
			List<BnbRoomPriceCalendar> calendar = room.getPriceCalendar();
			if (calendar == null) {
				calendar = new ArrayList<>();
				room.setPriceCalendar(calendar);
			} else {
				calendar.clear();
			}

			for (BnbRoomPriceRequestDto priceDto : request.getPriceCalendar()) {
				BnbRoomPriceCalendar entry = bnbMapper.toRoomPriceEntity(priceDto);
				entry.setRoom(room);
				calendar.add(entry);
			}
		}

		if (room.getImages() == null) {
			room.setImages(new ArrayList<>());
		}

		BnbRoom updated = bnbRoomJpa.save(room);

		Path basePath = Paths.get(ImageUtils.STORAGE_ROOT,
				ServiceType.BNB.name().toLowerCase(),
				service.getId().toString(),
				updated.getId().toString());

		fileService.updateImagesPathCustom(basePath, updated.getImages(), images);

		updated = bnbRoomJpa.save(updated);

		return bnbMapper.toRoomResponse(updated);
	}

	@Override
	public Page<BnbServiceResponseDto> getAllBnbServicesByProvider(long accountId, Pageable pageable) {
		// TODO Auto-generated method stub
		return null;
	}

	private BigDecimal calculateTotalAmount(BnbRoom room, LocalDate checkIn, LocalDate checkOut) {
		if (checkIn == null || checkOut == null || checkIn.isAfter(checkOut)) {
			return BigDecimal.ZERO;
		}

		// Meglio caricare solo i prezzi che possono interessare
		List<BnbRoomPriceCalendar> calendarEntries = bnbRoomPriceCalendarJpa
				.findOverlappingPriceRules(
						room.getId(),
						checkOut.minusDays(1), // ultimo giorno da considerare
						checkIn);

		BigDecimal basePrice = room.getBasePricePerNight();
		if (basePrice == null) {
			basePrice = BigDecimal.ZERO;
		}

		BigDecimal total = BigDecimal.ZERO;
		LocalDate current = checkIn;

		// Importante: usiamo isBefore(checkOut) → include la notte prima del check-out
		while (current.isBefore(checkOut)) {
			BigDecimal dayPrice = basePrice;

			for (BnbRoomPriceCalendar entry : calendarEntries) {
				if (!current.isBefore(entry.getStartDate()) &&
						!current.isAfter(entry.getEndDate())) {
					dayPrice = entry.getPricePerNight();
					break; // assumiamo che non ci siano sovrapposizioni
				}
			}

			total = total.add(dayPrice);
			current = current.plusDays(1);
		}

		return total.setScale(2, RoundingMode.HALF_UP); // opzionale, ma buona pratica
	}

	// Remaining existing methods...
}
