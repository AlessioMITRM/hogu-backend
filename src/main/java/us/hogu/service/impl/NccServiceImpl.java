package us.hogu.service.impl;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
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
import us.hogu.controller.dto.request.NccBookingRequestDto;
import us.hogu.controller.dto.request.NccSearchRequestDto;
import us.hogu.controller.dto.request.NccServiceRequestDto;
import us.hogu.controller.dto.request.RestaurantBookingRequestDto;
import us.hogu.controller.dto.response.DistanceResponseDto;
import us.hogu.controller.dto.response.GeoCoordinatesResponseDto;
import us.hogu.controller.dto.response.NccBookingResponseDto;
import us.hogu.controller.dto.response.NccManagementResponseDto;
import us.hogu.controller.dto.response.ServiceDetailResponseDto;
import us.hogu.controller.dto.response.ServiceSummaryResponseDto;
import us.hogu.converter.BookingMapper;
import us.hogu.converter.NccServiceMapper;
import us.hogu.converter.ServiceLocaleMapper;
import us.hogu.converter.ServiceMapper;
import us.hogu.exception.UserNotFoundException;
import us.hogu.exception.ValidationException;
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
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Service
@Transactional
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

	@Override
	@Transactional(readOnly = true)
	public Page<NccBookingResponseDto> getUserNccBookings(Long userId, Pageable pageable) {
		Page<NccBooking> bookings = nccBookingJpa.findByUserId(userId, pageable);
		return bookings.map(bookingMapper::toNccResponseDto);
	}

	@Override
	@Transactional(readOnly = true)
	public Page<ServiceSummaryResponseDto> getActiveNccServices(NccSearchRequestDto searchRequest, Pageable pageable) {
		String language = LocaleContextHolder.getLocale().getLanguage();
		String location = searchRequest.getDepartureLocation().trim();
		String city = "";
		String state = "";
		List<ServiceSummaryResponseDto> dtoList = new ArrayList<>();

		if (location.contains(",")) {
			String[] parts = location.split(",");
			if (parts.length == 2) {
				city = parts[0].trim();
				state = parts[1].trim();
			}
		}
		if (city.isBlank() || state.isBlank()) {
			throw new ValidationException(ErrorConstants.GENERIC_ERROR.name(),
					ErrorConstants.GENERIC_ERROR.getMessage());
		}

		Page<NccServiceEntity> entities = nccServiceJpa.findActiveBySearch(city, state,
				language.equalsIgnoreCase("it") ? "Italia" : "Italy", searchRequest.getPassengers(), language,
				pageable);

		if (!entities.isEmpty()) {
			// 2. Calcolo distanza tra origine e destinazione usando Stadia
			GeoCoordinatesResponseDto departureCoordinates = stadiaMapService
					.getCoordinatesFromAddress(searchRequest.getDepartureAddress() + ", " + city);
			GeoCoordinatesResponseDto destinationCoordinates = stadiaMapService
					.getCoordinatesFromAddress(searchRequest.getDestinationAddress() + ", " + city);

			DistanceResponseDto distanceDto = stadiaMapService.calculateDistance(departureCoordinates.getLatitude(),
					departureCoordinates.getLongitude(), destinationCoordinates.getLatitude(),
					destinationCoordinates.getLongitude());
			if (distanceDto == null) {
				throw new ValidationException(ErrorConstants.GENERIC_ERROR.name(),
						ErrorConstants.GENERIC_ERROR.getMessage());
			}

			Double km = distanceDto.getDistanceKm();

			for (NccServiceEntity entity : entities.getContent()) {
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
	public ServiceDetailResponseDto getNccServiceDetail(Long serviceId) {
		NccServiceEntity entity = nccServiceJpa.findDetailById(serviceId)
				.orElseThrow(() -> new ValidationException(ErrorConstants.SERVICE_NCC_NOT_FOUND.name(),
						ErrorConstants.SERVICE_NCC_NOT_FOUND.getMessage()));

		return nccServiceMapper.toDetailDto(entity);
	}

	@Override
	@Transactional(readOnly = true)
	public Page<ServiceSummaryResponseDto> searchNccServices(String searchTerm, Pageable pageable) {
		/*
		 * String language = LocaleContextHolder.getLocale().getLanguage();
		 * 
		 * Page<NccServiceEntity> entities =
		 * nccServiceJpa.findActiveBySearch(searchTerm, language, pageable);
		 * List<ServiceSummaryResponseDto> dtoList = new ArrayList<>();
		 * 
		 * for (NccServiceEntity entity : entities.getContent()) {
		 * dtoList.add(serviceMapper.toSummaryDto(entity)); }
		 * 
		 * return new PageImpl<>(dtoList, pageable, entities.getTotalElements());
		 */
		return null;
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

		checkNccAvailability(nccService, requestDto.getPickupTime());

		NccBooking booking = bookingMapper.toNccEntity(requestDto, user, nccService);
		NccBooking savedBooking = nccBookingJpa.save(booking);

		return bookingMapper.toNccResponseDto(savedBooking);
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
	public ServiceDetailResponseDto updateNccService(Long providerId, Long serviceId, NccServiceRequestDto requestDto,
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

		List<ServiceLocale> updatedLocales = serviceLocaleMapper.mapRequestToEntity(requestDto.getLocales());
		nccService.getLocales().clear();
		nccService.getLocales().addAll(updatedLocales);

		nccService.getVehiclesAvailable().clear();
		List<VehicleEntity> updatedVehicles = requestDto.getVehiclesAvailable().stream()
				.map(v -> VehicleEntity.builder().id(v.getId()).plateNumber(v.getPlateNumber()).model(v.getModel())
						.type(v.getType()).nccService(nccService).build())
				.collect(Collectors.toList());
		nccService.getVehiclesAvailable().addAll(updatedVehicles);

		NccServiceEntity savedService = nccServiceJpa.save(nccService);
		fileService.updateImages(nccService.getId(), ServiceType.NCC, nccService.getImages(), images);

		savedService = nccServiceJpa.save(savedService);

		return nccServiceMapper.toDetailDto(savedService, provider);
	}

	// ADMIN METHODS

	@Override
	@Transactional(readOnly = true)
	public List<NccManagementResponseDto> getAllNccServicesForAdmin() {
		return nccServiceMapper.toManagementDtoList(nccServiceJpa.findAllForAdmin());
	}

	@Transactional(readOnly = true)
	private void checkNccAvailability(NccServiceEntity nccService, OffsetDateTime pickupTime) {
		// Verifica se il servizio NCC è disponibile per l'orario richiesto
		List<BookingStatus> activeStatuses = Arrays.asList(BookingStatus.PENDING, BookingStatus.DEPOSIT_PAID,
				BookingStatus.WAITING_PROVIDER_CONFIRMATION);

		List<NccBooking> conflicts = nccBookingJpa.findConflictingBookings(nccService.getId(), pickupTime,
				activeStatuses);

		if (!conflicts.isEmpty()) {
			throw new ValidationException(ErrorConstants.NOT_AVAILABILITY_NCC_FOR_TIME.name(),
					ErrorConstants.NOT_AVAILABILITY_NCC_FOR_TIME.getMessage());
		}
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
