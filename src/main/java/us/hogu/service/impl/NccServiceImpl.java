package us.hogu.service.impl;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Comparator;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.management.ServiceNotFoundException;

import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import us.hogu.common.constants.ErrorConstants;
import us.hogu.common.util.ImageUtils;
import us.hogu.controller.dto.request.NccBookingEvent;
import us.hogu.controller.dto.request.NccBookingRequestDto;
import us.hogu.controller.dto.request.NccSearchRequestDto;
import us.hogu.controller.dto.request.NccServiceRequestDto;
import us.hogu.controller.dto.request.RestaurantBookingRequestDto;
import us.hogu.controller.dto.response.DistanceResponseDto;
import us.hogu.controller.dto.response.GeoCoordinatesResponseDto;
import us.hogu.controller.dto.response.InfoStatsDto;
import us.hogu.controller.dto.response.LuggageServiceDetailResponseDto;
import us.hogu.controller.dto.response.NccBookingResponseDto;
import us.hogu.controller.dto.response.NccBookingValidationResponseDto;
import us.hogu.controller.dto.response.NccDetailResponseDto;
import us.hogu.controller.dto.response.NccManagementResponseDto;
import us.hogu.controller.dto.response.ServiceDetailResponseDto;
import us.hogu.controller.dto.response.ServiceLocaleResponseDto;
import us.hogu.controller.dto.response.ServiceSummaryResponseDto;
import us.hogu.controller.dto.response.VehicleEntityResponseDto;
import us.hogu.converter.BookingMapper;
import us.hogu.converter.NccServiceMapper;
import us.hogu.converter.ServiceLocaleMapper;
import us.hogu.converter.ServiceMapper;
import us.hogu.exception.UserNotFoundException;
import us.hogu.exception.ValidationException;
import us.hogu.model.LuggageServiceEntity;
import us.hogu.model.NccBooking;
import us.hogu.model.NccServiceEntity;
import us.hogu.model.RestaurantBooking;
import us.hogu.model.RestaurantServiceEntity;
import us.hogu.model.ServiceLocale;
import us.hogu.model.User;
import us.hogu.model.UserServiceVerification;
import us.hogu.model.VehicleEntity;
import us.hogu.model.enums.BookingStatus;
import us.hogu.model.enums.ServiceType;
import us.hogu.model.enums.VerificationStatusServiceEY;
import us.hogu.repository.jpa.NccBookingJpa;
import us.hogu.repository.jpa.NccServiceJpa;
import us.hogu.repository.jpa.UserJpa;
import us.hogu.repository.jpa.UserServiceVerificationJpa;
import us.hogu.repository.projection.NccDetailProjection;
import us.hogu.repository.projection.NccManagementProjection;
import us.hogu.repository.projection.NccSummaryProjection;
import us.hogu.service.intefaces.FileService;
import us.hogu.service.intefaces.NccService;
import us.hogu.service.intefaces.StadiaMapService;
import us.hogu.service.mq.BookingProducer;
import us.hogu.service.redis.RedisAvailabilityService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import us.hogu.controller.dto.request.ServiceLocaleRequestDto;

@RequiredArgsConstructor
@Service
@Transactional
@Slf4j
public class NccServiceImpl implements NccService {
	private final NccServiceJpa nccServiceJpa;
	private final NccBookingJpa nccBookingJpa;
	private final UserJpa userJpa;
	private final UserServiceVerificationJpa userServiceVerificationJpa;
	private final FileService fileService;
	private final StadiaMapService stadiaMapService;
	private final NccServiceMapper nccServiceMapper;
	private final ServiceMapper serviceMapper;
	private final BookingMapper bookingMapper;
	private final ServiceLocaleMapper serviceLocaleMapper;
	private final RedisAvailabilityService redisService;
	private final BookingProducer bookingProducer;

	@Override
	@Transactional(readOnly = true)
	public Page<NccBookingResponseDto> getUserNccBookings(Long userId, Pageable pageable) {
		Page<NccBooking> bookings = nccBookingJpa.findByUserId(userId, pageable);
		return bookings.map(bookingMapper::toNccResponseDto);
	}

	private String buildCleanAddress(ServiceLocaleRequestDto addressDto) {
		if (addressDto == null)
			return "";

		StringBuilder sb = new StringBuilder();
		if (addressDto.getAddress() != null) {
			sb.append(addressDto.getAddress().trim());
		}

		String city = (addressDto.getCity() != null) ? addressDto.getCity().trim() : "";
		String province = (addressDto.getProvince() != null) ? addressDto.getProvince().trim() : "";
		String country = (addressDto.getCountry() != null) ? addressDto.getCountry().trim() : "";

		// Add City if not empty and not already in address
		if (!city.isEmpty() && !containsIgnoreCase(sb.toString(), city)) {
			if (sb.length() > 0)
				sb.append(", ");
			sb.append(city);
		}

		// Add Province if not empty, not same as City (to avoid Rome, Rome), and not
		// already in address
		if (!province.isEmpty() && !province.equalsIgnoreCase(city) && !containsIgnoreCase(sb.toString(), province)) {
			if (sb.length() > 0)
				sb.append(", ");
			sb.append(province);
		}

		// Add Country if not empty and not already in address
		if (!country.isEmpty() && !containsIgnoreCase(sb.toString(), country)) {
			if (sb.length() > 0)
				sb.append(", ");
			sb.append(country);
		}

		return sb.toString();
	}

	private boolean containsIgnoreCase(String src, String what) {
		if (src == null || what == null)
			return false;
		return src.toLowerCase().contains(what.toLowerCase());
	}

	@Override
	@Transactional(readOnly = true)
	public Page<ServiceSummaryResponseDto> getActiveNccServices(NccSearchRequestDto searchRequest, Pageable pageable) {
		String language = "en"; // Forced to 'en' as per user instruction (DB only has English)
		List<ServiceSummaryResponseDto> dtoList = new ArrayList<>();

		String province = null;
		String country = null;
		String cityForGeo = "";

		if (searchRequest.getDepartureAddress() != null) {
			if (searchRequest.getDepartureAddress().getProvince() != null
					&& !searchRequest.getDepartureAddress().getProvince().trim().isEmpty()) {
				province = searchRequest.getDepartureAddress().getProvince().trim();
			}
			if (searchRequest.getDepartureAddress().getCountry() != null
					&& !searchRequest.getDepartureAddress().getCountry().trim().isEmpty()) {
				country = searchRequest.getDepartureAddress().getCountry().trim();
			}
			if (searchRequest.getDepartureAddress().getCity() != null
					&& !searchRequest.getDepartureAddress().getCity().trim().isEmpty()) {
				cityForGeo = searchRequest.getDepartureAddress().getCity().trim();
			}
		}

		Page<NccServiceEntity> entities = nccServiceJpa.findActiveByLocation(province, country, language, pageable);

		if (!entities.isEmpty()) {
			// Construct address for geocoding
			String departureFullAddress = buildCleanAddress(searchRequest.getDepartureAddress());
			String destinationFullAddress = buildCleanAddress(searchRequest.getDestinationAddress());

			// 2. Calcolo distanza tra origine e destinazione usando Stadia

			log.info("Calculating distance for NCC Search. Departure: '{}', Destination: '{}'", departureFullAddress,
					destinationFullAddress);

			GeoCoordinatesResponseDto departureCoordinates = stadiaMapService
					.getCoordinatesFromAddress(departureFullAddress);
			log.info("Departure Coordinates resolved: Lat={}, Lon={}, Address='{}'",
					departureCoordinates.getLatitude(), departureCoordinates.getLongitude(),
					departureCoordinates.getFullAddress());

			GeoCoordinatesResponseDto destinationCoordinates = stadiaMapService
					.getCoordinatesFromAddress(destinationFullAddress);
			log.info("Destination Coordinates resolved: Lat={}, Lon={}, Address='{}'",
					destinationCoordinates.getLatitude(), destinationCoordinates.getLongitude(),
					destinationCoordinates.getFullAddress());

			DistanceResponseDto distanceDto = stadiaMapService.calculateDistance(departureCoordinates.getLatitude(),
					departureCoordinates.getLongitude(), destinationCoordinates.getLatitude(),
					destinationCoordinates.getLongitude());

			log.info("Distance calculated: {} km, {} minutes", distanceDto.getDistanceKm(),
					distanceDto.getDurationMinutes());
			if (distanceDto == null) {
				throw new ValidationException(ErrorConstants.GENERIC_ERROR.name(),
						ErrorConstants.GENERIC_ERROR.getMessage());
			}

			Double km = distanceDto.getDistanceKm();

			// Parse date and time for availability check
			OffsetDateTime pickupDateTime = null;
			if (searchRequest.getDepartureDate() != null && !searchRequest.getDepartureDate().isBlank() &&
					searchRequest.getDepartureTime() != null && !searchRequest.getDepartureTime().isBlank()) {
				try {
					DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
					LocalDate date = LocalDate.parse(searchRequest.getDepartureDate(), dateFormatter);
					LocalTime time = LocalTime.parse(searchRequest.getDepartureTime());
					pickupDateTime = LocalDateTime.of(date, time).atOffset(ZoneOffset.UTC);
				} catch (Exception e) {

				}
			}

			for (NccServiceEntity entity : entities.getContent()) {
				// Check passengers capacity
				if (searchRequest.getPassengers() != null && entity.getVehiclesAvailable() != null) {
					boolean hasCapacity = entity.getVehiclesAvailable().stream()
							.anyMatch(v -> v.getNumberOfSeats() >= searchRequest.getPassengers());
					if (!hasCapacity)
						continue;
				}

				// Check Redis availability
				if (pickupDateTime != null) {
					int maxCapacity = entity.getVehiclesAvailable() != null ? entity.getVehiclesAvailable().size() : 0;
					if (!redisService.checkNccAvailability(entity.getId(), pickupDateTime, maxCapacity)) {
						continue;
					}
				}

				ServiceSummaryResponseDto dto = serviceMapper.toSummaryDto(entity);
				VehicleEntity vehicle = entity.getVehiclesAvailable().get(0);
				dto.setModel(vehicle.getModel());
				dto.setNumberOfSeats(vehicle.getNumberOfSeats());

				BigDecimal basePrice = entity.getBasePrice();
				BigDecimal distance = BigDecimal.valueOf(km != null ? km : 0);
				BigDecimal estimatedPrice = basePrice.multiply(distance);

				dto.setEstimatedPrice(estimatedPrice);
				dto.setDistanceKm(km);

				dtoList.add(dto);
			}
		}

		return new PageImpl<>(dtoList, pageable, entities.getTotalElements());
	}

	@Override
	@Transactional(readOnly = true)
	public ServiceDetailResponseDto getNccServiceDetail(Long serviceId, String dateStr, String timeStr,
			Integer passengers, 
			String from, String fromCity, String fromProvince, String fromCountry,
			String to, String toCity, String toProvince, String toCountry,
			String tripType) {
		String language = LocaleContextHolder.getLocale().getLanguage();
		NccServiceEntity entity = nccServiceJpa.findDetailById(serviceId, language)
				.orElseThrow(() -> new ValidationException(ErrorConstants.SERVICE_NCC_NOT_FOUND.name(),
						ErrorConstants.SERVICE_NCC_NOT_FOUND.getMessage()));

		ServiceDetailResponseDto dto = nccServiceMapper.toDetailDto(entity);

		// Calculate estimated price if addresses are provided
		if (from != null && !from.isBlank() && to != null && !to.isBlank()) {
			try {
				// Construct clean full addresses for Geocoding using the new parameters
				ServiceLocaleRequestDto fromDto = new ServiceLocaleRequestDto();
				fromDto.setAddress(from);
				fromDto.setCity(fromCity);
				fromDto.setProvince(fromProvince);
				fromDto.setCountry(fromCountry);
				String fromFull = buildCleanAddress(fromDto);
				
				ServiceLocaleRequestDto toDto = new ServiceLocaleRequestDto();
				toDto.setAddress(to);
				toDto.setCity(toCity);
				toDto.setProvince(toProvince);
				toDto.setCountry(toCountry);
				String toFull = buildCleanAddress(toDto);
				
				// Fallback if buildCleanAddress returns empty (should not happen if from/to are set)
				if (fromFull.isBlank()) fromFull = from;
				if (toFull.isBlank()) toFull = to;

				GeoCoordinatesResponseDto departureCoordinates = stadiaMapService.getCoordinatesFromAddress(fromFull);
				GeoCoordinatesResponseDto destinationCoordinates = stadiaMapService.getCoordinatesFromAddress(toFull);

				if (departureCoordinates != null && destinationCoordinates != null) {
					dto.setPickupLatitude(departureCoordinates.getLatitude());
					dto.setPickupLongitude(departureCoordinates.getLongitude());
					dto.setDestinationLatitude(destinationCoordinates.getLatitude());
					dto.setDestinationLongitude(destinationCoordinates.getLongitude());

					DistanceResponseDto distanceDto = stadiaMapService.calculateDistance(
							departureCoordinates.getLatitude(),
							departureCoordinates.getLongitude(),
							destinationCoordinates.getLatitude(),
							destinationCoordinates.getLongitude());

					if (distanceDto != null) {
						BigDecimal distance = BigDecimal.valueOf(distanceDto.getDistanceKm());
						BigDecimal basePrice = entity.getBasePrice();
						BigDecimal estimatedPrice = basePrice.multiply(distance);
						dto.setEstimatedPrice(estimatedPrice);
						dto.setDistanceKm(distanceDto.getDistanceKm());
					}
				}
			} catch (Exception e) {
				log.error("Error calculating estimated price in detail: {}", e.getMessage());
			}
		}

		// Check availability if parameters are provided
		if (dateStr != null && !dateStr.isBlank() && timeStr != null && !timeStr.isBlank()) {
			try {
				DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
				LocalDate date = LocalDate.parse(dateStr, dateFormatter);
				LocalTime time = LocalTime.parse(timeStr);
				OffsetDateTime pickupDateTime = LocalDateTime.of(date, time).atOffset(ZoneOffset.UTC);

				// Check passengers capacity first
				if (passengers != null && entity.getVehiclesAvailable() != null) {
					boolean hasCapacity = entity.getVehiclesAvailable().stream()
							.anyMatch(v -> v.getNumberOfSeats() >= passengers);
					if (!hasCapacity) {
						dto.setAvailable(false);
						return dto;
					}
				}

				// Check Redis availability
				int maxCapacity = entity.getVehiclesAvailable() != null ? entity.getVehiclesAvailable().size() : 0;
				boolean isAvailable = redisService.checkNccAvailability(entity.getId(), pickupDateTime, maxCapacity);
				dto.setAvailable(isAvailable);

			} catch (Exception e) {
				// In case of parsing error, we keep the default availability (true) or handle
				// as needed
				// For now silently fail to default
			}
		}

		return dto;
	}

	@Override
	@Transactional
	public NccBookingResponseDto createNccBooking(NccBookingRequestDto requestDto, Long userId) {
		User user = userJpa.findById(userId)
				.orElseThrow(() -> new ValidationException(ErrorConstants.USER_NOT_FOUND.name(),
						ErrorConstants.USER_NOT_FOUND.getMessage()));

		NccServiceEntity nccService = nccServiceJpa.findById(requestDto.getNccServiceId())
				.orElseThrow(() -> new ValidationException(ErrorConstants.SERVICE_NCC_NOT_FOUND.name(),
						ErrorConstants.SERVICE_NCC_NOT_FOUND.getMessage()));

		// 1. Check Capacity (Number of vehicles)
		int maxCapacity = nccService.getVehiclesAvailable() != null ? nccService.getVehiclesAvailable().size() : 0;
		if (maxCapacity == 0) {
			throw new ValidationException(ErrorConstants.NOT_AVAILABILITY_NCC_FOR_TIME.name(),
					"Nessun veicolo disponibile per questo servizio");
		}

		// 2. Redis Atomic Reservation
		boolean reserved = redisService.reserveNcc(nccService.getId(), requestDto.getPickupTime(), maxCapacity);

		if (!reserved) {
			throw new ValidationException(ErrorConstants.NOT_AVAILABILITY_NCC_FOR_TIME.name(),
					ErrorConstants.NOT_AVAILABILITY_NCC_FOR_TIME.getMessage());
		}

		// 3. Create Booking (Sync)
		NccBooking booking = bookingMapper.toNccEntity(requestDto, user, nccService);
		booking.setStatus(BookingStatus.PENDING);
		booking.setBillingFirstName(requestDto.getBillingFirstName());
		booking.setBillingLastName(requestDto.getBillingLastName());
		booking.setBillingTaxCode(requestDto.getFiscalCode());
		booking.setBillingVatNumber(requestDto.getTaxId());
		booking.setBillingAddress(requestDto.getBillingAddress());
		booking.setBillingEmail(requestDto.getBillingEmail());

		NccBooking savedBooking;
		try {
			savedBooking = nccBookingJpa.save(booking);
		} catch (Exception e) {
			redisService.rollbackNcc(nccService.getId(), requestDto.getPickupTime());
			throw e;
		}

		// 4. Async Event for DB Persistence/Processing
		NccBookingEvent queueEvent = NccBookingEvent.builder()
				.userId(userId)
				.nccServiceId(nccService.getId())
				.bookingId(savedBooking.getId())
				.pickupTime(requestDto.getPickupTime())
				.pickupLocation(requestDto.getPickupLocation())
				.destination(requestDto.getDestination())
				.totalAmount(requestDto.getTotalAmount())
				.billingFirstName(user.getName())
				.billingLastName(user.getSurname())
				.pickupLatitude(requestDto.getPickupLatitude())
				.pickupLongitude(requestDto.getPickupLongitude())
				.destinationLatitude(requestDto.getDestinationLatitude())
				.destinationLongitude(requestDto.getDestinationLongitude())
				.build();

		try {
			bookingProducer.sendNccBookingRequest(queueEvent);
		} catch (Exception e) {
			// Log error, booking is already saved in PENDING
		}

		return bookingMapper.toNccResponseDto(savedBooking);
	}

	@Override
	@Transactional(readOnly = true)
	public InfoStatsDto getInfo(Long providerId) {
		NccServiceEntity entity = nccServiceJpa.findByProviderIdForSingleService(providerId)
				.orElseThrow(() -> new ValidationException(
						ErrorConstants.SERVICE_NCC_NOT_FOUND.name(),
						ErrorConstants.SERVICE_NCC_NOT_FOUND.getMessage()));

		InfoStatsDto infoStats = nccServiceJpa.getInfoStatsByProviderId(providerId);

		infoStats.setServiceId(entity.getId());

		return infoStats;
	}

	@Override
	@Transactional(readOnly = true)
	public NccDetailResponseDto getNccServiceByServiceIdAndProviderId(Long serviceId, Long providerId) {
		NccServiceEntity entity = nccServiceJpa.findDetailByIdAndProvider(serviceId, providerId)
				.orElseThrow(() -> new ValidationException(
						ErrorConstants.SERVICE_NCC_NOT_FOUND.name(),
						ErrorConstants.SERVICE_NCC_NOT_FOUND.getMessage()));

		ServiceLocale locale = entity.getLocales().get(0);
		VehicleEntity vehicle = entity.getVehiclesAvailable().get(0);

		return NccDetailResponseDto.builder()
				.name(entity.getName())
				.description(entity.getDescription())
				.basePrice(entity.getBasePrice())
				.publicationStatus(entity.getPublicationStatus())

				.locale(ServiceLocaleResponseDto.builder()
						.serviceId(locale.getId())
						.serviceType(locale.getServiceType())
						.language(locale.getLanguage())
						.country(locale.getCountry())
						.state(locale.getState())
						.city(locale.getCity())
						.address(locale.getAddress())
						.build())

				.vehicle(VehicleEntityResponseDto.builder()
						.id(vehicle.getId())
						.numberOfSeats(vehicle.getNumberOfSeats())
						.plateNumber(vehicle.getPlateNumber())
						.model(vehicle.getModel())
						.type(vehicle.getType())
						.build())

				.images(entity.getImages())
				.build();

	}

	@Override
	@Transactional(readOnly = true)
	public Page<NccManagementResponseDto> getProviderNccServices(Long providerId, Pageable pageable) {
		Page<NccServiceEntity> page = nccServiceJpa.findByProviderId(providerId, pageable);
		return page.map(nccServiceMapper::toManagementDto);
	}

	@Override
	@Transactional(readOnly = true)
	public List<NccBookingResponseDto> getNccBookings(Long serviceId, Long providerId) {
		nccServiceJpa.findByIdAndUserId(serviceId, providerId).orElseThrow(
				() -> new ValidationException(ErrorConstants.SERVICE_NCC_NOT_FOUND_OR_NOT_AUTHORIZED.name(),
						ErrorConstants.SERVICE_NCC_NOT_FOUND_OR_NOT_AUTHORIZED.getMessage()));

		List<NccBooking> bookings = nccBookingJpa.findByNccServiceId(serviceId);
		return bookings.stream().map(bookingMapper::toNccResponseDto).collect(Collectors.toList());
	}

	@Override
	@Transactional(readOnly = true)
	public List<NccBookingResponseDto> getNccFullyPaidBookings(Long serviceId, Long providerId) {
		nccServiceJpa.findByIdAndUserId(serviceId, providerId).orElseThrow(
				() -> new ValidationException(ErrorConstants.SERVICE_NCC_NOT_FOUND_OR_NOT_AUTHORIZED.name(),
						ErrorConstants.SERVICE_NCC_NOT_FOUND_OR_NOT_AUTHORIZED.getMessage()));

		List<NccBooking> bookings = nccBookingJpa.findByNccServiceId(serviceId).stream()
				.filter(b -> b.getStatus() == BookingStatus.COMPLETED
						|| b.getStatus() == BookingStatus.WAITING_COMPLETION
						|| b.getStatus() == BookingStatus.WAITING_CUSTOMER_PAYMENT
						|| b.getStatus() == BookingStatus.FULL_PAYMENT_COMPLETED)
				.collect(Collectors.toList());
		bookings.sort(Comparator.comparing(NccBooking::getPickupTime, Comparator.nullsLast(Comparator.naturalOrder())));
		return bookings.stream().map(bookingMapper::toNccResponseDto).collect(Collectors.toList());
	}

	@Override
	@Transactional(readOnly = true)
	public Page<NccBookingResponseDto> getNccBookingsHistory(Long serviceId, Long providerId, Pageable pageable) {
		nccServiceJpa.findByIdAndUserId(serviceId, providerId).orElseThrow(
				() -> new ValidationException(ErrorConstants.SERVICE_NCC_NOT_FOUND_OR_NOT_AUTHORIZED.name(),
						ErrorConstants.SERVICE_NCC_NOT_FOUND_OR_NOT_AUTHORIZED.getMessage()));

		OffsetDateTime now = OffsetDateTime.now().withHour(0).withMinute(0).withSecond(0).withNano(0);
		Page<NccBooking> bookings = nccBookingJpa.findPastBookings(serviceId, now, pageable);

		List<NccBookingResponseDto> dtoList = bookings.getContent().stream()
				.map(bookingMapper::toNccResponseDto)
				.collect(Collectors.toList());

		return new PageImpl<>(dtoList, pageable, bookings.getTotalElements());
	}

	@Override
	@Transactional(readOnly = true)
	public NccBookingResponseDto getCurrentNccBooking(Long serviceId, Long providerId) {
		nccServiceJpa.findByIdAndUserId(serviceId, providerId).orElseThrow(
				() -> new ValidationException(ErrorConstants.SERVICE_NCC_NOT_FOUND_OR_NOT_AUTHORIZED.name(),
						ErrorConstants.SERVICE_NCC_NOT_FOUND_OR_NOT_AUTHORIZED.getMessage()));

		List<NccBooking> bookings = nccBookingJpa.findByNccServiceId(serviceId).stream()
				.filter(b -> b.getStatus() == BookingStatus.COMPLETED
						|| b.getStatus() == BookingStatus.WAITING_COMPLETION
						|| b.getStatus() == BookingStatus.FULL_PAYMENT_COMPLETED
						|| b.getStatus() == BookingStatus.WAITING_CUSTOMER_PAYMENT)
				.collect(Collectors.toList());
		if (bookings.isEmpty()) {
			return null;
		}

		LocalDate today = LocalDate.now();

		return bookings.stream()
				.filter(b -> b.getPickupTime() != null
						&& b.getPickupTime().toLocalDate().isEqual(today))
				.sorted(Comparator.comparing(NccBooking::getPickupTime))
				.findFirst()
				.map(bookingMapper::toNccResponseDto)
				.orElse(null);
	}

	@Override
	@Transactional
	public void rectifyBooking(Long providerId, Long bookingId, BigDecimal newPrice, String note) {
		if (newPrice == null || newPrice.compareTo(BigDecimal.ZERO) <= 0) {
			throw new ValidationException(
					ErrorConstants.INVALID_AMOUNT.name(),
					ErrorConstants.INVALID_AMOUNT.getMessage());
		}

		boolean authorized = nccBookingJpa.existsByIdAndNccServiceUserId(bookingId, providerId);
		if (!authorized) {
			throw new ValidationException(
					ErrorConstants.BOOKING_NOT_FOUND_OR_NOT_AUTHORIZED.name(),
					ErrorConstants.BOOKING_NOT_FOUND_OR_NOT_AUTHORIZED.getMessage());
		}

		NccBooking booking = nccBookingJpa.findById(bookingId)
				.orElseThrow(() -> new ValidationException(
						ErrorConstants.BOOKING_NCC_NOT_FOUND.name(),
						ErrorConstants.BOOKING_NCC_NOT_FOUND.getMessage()));

		BookingStatus status = booking.getStatus();
		if (status != BookingStatus.PENDING
				&& status != BookingStatus.PAYMENT_AUTHORIZED
				&& status != BookingStatus.WAITING_PROVIDER_CONFIRMATION
				&& status != BookingStatus.WAITING_CUSTOMER_PAYMENT) {
			throw new ValidationException(
					ErrorConstants.GENERIC_ERROR.name(),
					ErrorConstants.GENERIC_ERROR.getMessage());
		}

		booking.setTotalAmount(newPrice);
		booking.setStatus(BookingStatus.WAITING_CUSTOMER_PAYMENT);
		booking.setStatusReason(note);

		nccBookingJpa.save(booking);
	}

	@Override
	@Transactional(readOnly = true)
	public NccBookingValidationResponseDto validateNccBookingByCode(Long providerId, String code) {
		if (code == null || code.trim().isEmpty()) {
			return NccBookingValidationResponseDto.builder().valid(false).build();
		}

		String digits = code.replaceAll("[^0-9]", "");
		if (digits.isEmpty()) {
			return NccBookingValidationResponseDto.builder().valid(false).build();
		}

		Long bookingId;
		try {
			bookingId = Long.parseLong(digits);
		} catch (NumberFormatException e) {
			return NccBookingValidationResponseDto.builder().valid(false).build();
		}

		NccBooking booking = nccBookingJpa.findById(bookingId).orElse(null);
		if (booking == null) {
			return NccBookingValidationResponseDto.builder()
					.valid(false)
					.bookingId(bookingId)
					.build();
		}

		if (booking.getNccService() == null
				|| booking.getNccService().getUser() == null
				|| !booking.getNccService().getUser().getId().equals(providerId)) {
			return NccBookingValidationResponseDto.builder()
					.valid(false)
					.bookingId(bookingId)
					.build();
		}

		LocalDate today = LocalDate.now();
		LocalDate bookingDate = booking.getPickupTime() != null
				? booking.getPickupTime().toLocalDate()
				: null;

		boolean dateOk = bookingDate != null && bookingDate.isEqual(today);
		boolean statusOk = booking.getStatus() == BookingStatus.WAITING_COMPLETION
				|| booking.getStatus() == BookingStatus.COMPLETED
				|| booking.getStatus() == BookingStatus.FULL_PAYMENT_COMPLETED;

		boolean valid = statusOk && dateOk;

		String firstName = booking.getBillingFirstName() != null ? booking.getBillingFirstName()
				: (booking.getUser() != null ? booking.getUser().getName() : null);
		String lastName = booking.getBillingLastName() != null ? booking.getBillingLastName()
				: (booking.getUser() != null ? booking.getUser().getSurname() : null);
		String fullName = ((firstName != null ? firstName : "") + " " + (lastName != null ? lastName : "")).trim();

		String dateStr = booking.getPickupTime() != null
				? booking.getPickupTime().toLocalDate().toString()
				: null;
		String timeStr = booking.getPickupTime() != null
				? booking.getPickupTime().toLocalTime().format(DateTimeFormatter.ofPattern("HH:mm"))
				: null;

		return NccBookingValidationResponseDto.builder()
				.valid(valid)
				.bookingId(booking.getId())
				.firstName(firstName)
				.lastName(lastName)
				.fullName(fullName)
				.date(dateStr)
				.time(timeStr)
				.passengers(booking.getPassengers())
				.pickupLocation(booking.getPickupLocation())
				.destination(booking.getDestination())
				.build();
	}

	@Override
	@Transactional
	public ServiceDetailResponseDto createNccService(Long providerId, NccServiceRequestDto request,
			List<MultipartFile> images) throws Exception {
		User provider = userJpa.findById(providerId)
				.orElseThrow(() -> new UserNotFoundException("Fornitore non trovato"));

		Boolean providerIsAllowedForService = userServiceVerificationJpa
				.existsByUserIdAndServiceTypeAndVerificationStatus(provider.getId(), ServiceType.NCC,
						VerificationStatusServiceEY.ACTIVE);
		if (!providerIsAllowedForService) {
			throw new ValidationException(ErrorConstants.PROVIDER_NOT_ALLOWED.name(),
					ErrorConstants.PROVIDER_NOT_ALLOWED.getMessage());
		}

		NccServiceEntity nccService = nccServiceMapper.toEntity(request);
		nccService.setUser(provider);

		NccServiceEntity savedService = nccServiceJpa.save(nccService);
		savedService.setImages(new ArrayList<String>());

		fileService.uploadImages(savedService.getId(), ServiceType.NCC, savedService.getImages(), images);

		savedService = nccServiceJpa.save(savedService);

		return nccServiceMapper.toDetailDto(savedService, savedService.getUser());
	}

	@Override
	@Transactional
	public NccDetailResponseDto updateNccService(Long providerId, Long serviceId, NccServiceRequestDto requestDto,
			List<MultipartFile> images) throws Exception {
		User provider = userJpa.findById(providerId)
				.orElseThrow(() -> new UserNotFoundException(ErrorConstants.PROVIDER_NOT_FOUND.getMessage()));

		NccServiceEntity nccService = nccServiceJpa.findById(serviceId)
				.orElseThrow(() -> new ValidationException(ErrorConstants.SERVICE_NOT_FOUND.name(),
						ErrorConstants.SERVICE_NOT_FOUND.getMessage()));
		if (nccService.getUser().getId() != providerId) {
			throw new ValidationException(ErrorConstants.PROVIDER_NOT_ALLOWED.name(),
					ErrorConstants.PROVIDER_NOT_ALLOWED.getMessage());
		}

		nccService.setName(requestDto.getName());
		nccService.setDescription(requestDto.getDescription());
		nccService.setBasePrice(requestDto.getBasePrice());
		nccService.setPublicationStatus(requestDto.getPublicationStatus());

		if (requestDto.getLocales() != null) {
			List<ServiceLocale> updatedLocales = serviceLocaleMapper.mapRequestToEntity(requestDto.getLocales());
			nccService.getLocales().clear();
			nccService.getLocales().addAll(updatedLocales);
		}

		// Vehicle handling - single vehicle in request
		if (requestDto.getVehicle() != null) {
			nccService.getVehiclesAvailable().clear();
			VehicleEntity vehicleEntity = VehicleEntity.builder()
					.id(requestDto.getVehicle().getId())
					.numberOfSeats(requestDto.getVehicle().getNumberOfSeats())
					.plateNumber(requestDto.getVehicle().getPlateNumber())
					.model(requestDto.getVehicle().getModel())
					.type(requestDto.getVehicle().getType())
					.nccService(nccService)
					.build();
			nccService.getVehiclesAvailable().add(vehicleEntity);
		}

		NccServiceEntity savedService = nccServiceJpa.save(nccService);

		fileService.updateImages(nccService.getId(), ServiceType.NCC, nccService.getImages(), images);

		savedService = nccServiceJpa.save(savedService);

		return nccServiceMapper.toNccDetailDto(savedService);
	}

	// ADMIN METHODS

	@Override
	@Transactional(readOnly = true)
	public List<NccManagementResponseDto> getAllNccServicesForAdmin() {
		return nccServiceMapper.toManagementDtoList(nccServiceJpa.findAllForAdmin());
	}

	@Override
	@Transactional
	public void approveNccService(Long serviceId) {
		NccServiceEntity nccService = nccServiceJpa.findById(serviceId)
				.orElseThrow(() -> new ValidationException(ErrorConstants.SERVICE_NCC_NOT_FOUND.name(),
						ErrorConstants.SERVICE_NCC_NOT_FOUND.getMessage()));

		nccService.setPublicationStatus(true);
		nccServiceJpa.save(nccService);
	}
}
