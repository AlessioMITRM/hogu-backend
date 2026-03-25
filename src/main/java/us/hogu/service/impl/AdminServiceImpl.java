package us.hogu.service.impl;

import java.util.ArrayList;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import us.hogu.common.constants.ErrorConstants;
import us.hogu.controller.dto.response.AdminCustomerDetailResponseDto;
import us.hogu.controller.dto.response.AdminCustomerResponseDto;
import us.hogu.controller.dto.response.AdminDashboardKpiResponseDto;
import us.hogu.controller.dto.response.AdminBookingDetailResponseDto;
import us.hogu.controller.dto.response.AdminBookingResponseDto;
import us.hogu.controller.dto.response.AdminProviderResponseDto;
import us.hogu.controller.dto.response.UserDocumentResponseDto;
import us.hogu.exception.ValidationException;
import us.hogu.model.BnbServiceEntity;
import us.hogu.model.Booking; // Aggiunto import Booking
import us.hogu.model.ClubServiceEntity;
import us.hogu.model.LuggageServiceEntity;
import us.hogu.model.NccServiceEntity;
import us.hogu.model.RestaurantServiceEntity;
import us.hogu.model.User;
import us.hogu.model.UserDocument;
import us.hogu.model.UserServiceVerification;
import us.hogu.model.enums.BookingStatus;
import us.hogu.model.enums.UserRole;
import us.hogu.model.enums.UserStatus;
import us.hogu.model.enums.VerificationStatusServiceEY;
import us.hogu.repository.jpa.BookingJpa; // Aggiunto import BookingJpa
import us.hogu.repository.jpa.BnbBookingJpa;
import us.hogu.repository.jpa.BnbServiceJpa;
import us.hogu.repository.jpa.ClubServiceJpa;
import us.hogu.repository.jpa.LuggageServiceJpa;
import us.hogu.repository.jpa.NccServiceJpa;
import us.hogu.repository.jpa.RestaurantServiceJpa;
import us.hogu.repository.jpa.UserDocumentJpa;
import us.hogu.repository.jpa.UserJpa;
import us.hogu.repository.jpa.UserOtpJpa;
import us.hogu.repository.jpa.UserServiceVerificationJpa;
import us.hogu.repository.projection.UserDocumentForGetAllProjection;
import us.hogu.service.availability.AvailabilityBookingStatusPolicy;
import us.hogu.service.intefaces.AdminService;
import us.hogu.service.intefaces.EmailService;
import us.hogu.service.intefaces.FileService;
import us.hogu.service.redis.RedisAvailabilityService;
import us.hogu.service.redis.UserStatusRedisService;

@RequiredArgsConstructor
@Service
public class AdminServiceImpl implements AdminService {
	private final UserJpa userJpa;
	private final UserOtpJpa userOtpJpa;
	private final UserServiceVerificationJpa userServiceVerificationJpa;
	private final UserDocumentJpa userDocumentJpa;
	private final NccServiceJpa nccServiceJpa;
	private final RestaurantServiceJpa restaurantServiceJpa;
	private final ClubServiceJpa clubServiceJpa;
	private final LuggageServiceJpa luggageServiceJpa;
	private final BnbServiceJpa bnbServiceJpa;
	private final BnbBookingJpa bnbBookingJpa;
	private final BookingJpa bookingJpa;
	private final EmailService emailService;
	private final FileService fileService;
	private final UserStatusRedisService userStatusRedisService;
	private final RedisAvailabilityService redisAvailabilityService;

	@Override
	@Transactional(readOnly = true)
	public List<us.hogu.controller.dto.response.PendingVerificationResponseDto> getPendingVerifications() {
		List<UserServiceVerification> pendingVerifications = userServiceVerificationJpa
				.findByVerificationStatus(VerificationStatusServiceEY.PENDING);

		List<us.hogu.controller.dto.response.PendingVerificationResponseDto> response = new ArrayList<>();

		for (UserServiceVerification verification : pendingVerifications) {
			us.hogu.controller.dto.response.PendingVerificationResponseDto dto = new us.hogu.controller.dto.response.PendingVerificationResponseDto();
			dto.setVerificationId(verification.getId());
			dto.setUserId(verification.getUser().getId());
			String fullName = verification.getUser().getName();
			if (verification.getUser().getSurname() != null && !verification.getUser().getSurname().isEmpty()) {
				fullName += " " + verification.getUser().getSurname();
			}
			dto.setProviderName(fullName);
			dto.setEmail(verification.getUser().getEmail());
			dto.setServiceType(verification.getServiceType());
			dto.setRequestDate(verification.getCreationDate());
			dto.setStatus(verification.getVerificationStatus());
			dto.setLicenseValid(verification.isLicenseValid());
			dto.setVatValid(verification.isVatValid());
			dto.setDescription(verification.getDescription());
			dto.setIban(verification.getUser().getIban());

			List<UserDocumentForGetAllProjection> userDocuments = userDocumentJpa.findForGetAll(verification);
			List<UserDocumentResponseDto> docDtos = new ArrayList<>();
			for (UserDocumentForGetAllProjection doc : userDocuments) {
				UserDocumentResponseDto docDto = new UserDocumentResponseDto();
				docDto.setId(doc.getId());
				docDto.setFilename(doc.getFilename());
				docDto.setApproved(doc.isApproved());
				docDtos.add(docDto);
			}
			dto.setDocuments(docDtos);

			response.add(dto);
		}

		return response;
	}

	@Override
	@Transactional(readOnly = true)
	public UserDocumentResponseDto getFileUserDocument(Long idDocument) {
		UserDocument userDocument = userDocumentJpa.findById(idDocument)
				.orElseThrow(() -> new ValidationException(ErrorConstants.DOCUMENT_NOT_FOUND.name(),
						ErrorConstants.DOCUMENT_NOT_FOUND.getMessage()));

		UserDocumentResponseDto userDocumentResponseDto = new UserDocumentResponseDto();
		userDocumentResponseDto.setId(userDocument.getId());
		userDocumentResponseDto.setFilename(userDocument.getFilename());
		userDocumentResponseDto.setFileData(userDocument.getFileData());
		userDocumentResponseDto.setApproved(userDocument.isApproved());

		return userDocumentResponseDto;
	}

	@Override
	@Transactional(readOnly = true)
	public Page<AdminCustomerResponseDto> getCustomers(String search, int page, int size) {
		Pageable pageable = PageRequest.of(page, size);
		List<UserStatus> statuses = List.of(
				UserStatus.ACTIVE,
				UserStatus.SUSPENDED,
				UserStatus.DEACTIVATED,
				UserStatus.PENDING_ADMIN_APPROVAL,
				UserStatus.BANNED);

		Page<User> userPage = userJpa.findCustomers(UserRole.CUSTOMER, statuses, search, pageable);

		return userPage.map(user -> AdminCustomerResponseDto.builder()
				.id(user.getId())
				.name(user.getName())
				.surname(user.getSurname())
				.email(user.getEmail())
				.status(user.getStatus())
				.creationDate(user.getCreationDate())
				.build());
	}

	@Override
	@Transactional(readOnly = true)
	public Page<AdminProviderResponseDto> getProviders(String search, int page, int size) {
		Pageable pageable = PageRequest.of(page, size);
		List<UserStatus> statuses = List.of(
				UserStatus.ACTIVE,
				UserStatus.SUSPENDED,
				UserStatus.DEACTIVATED,
				UserStatus.PENDING_ADMIN_APPROVAL,
				UserStatus.BANNED);

		Page<User> userPage = userJpa.findProviders(UserRole.PROVIDER, statuses, search, pageable);

		return userPage.map(user -> {
			AdminProviderResponseDto dto = AdminProviderResponseDto.builder()
					.id(user.getId())
					.name(user.getName())
					.email(user.getEmail())
					.status(user.getStatus())
					.creationDate(user.getCreationDate())
					.iban(user.getIban())
					.build();

			userServiceVerificationJpa.findByUser(user).ifPresent(verification -> {
				dto.setDescription(verification.getDescription());
			});

			return dto;
		});
	}

	@Override
	@Transactional
	public void approveServiceVerification(Long verificationId) {
		UserServiceVerification verification = userServiceVerificationJpa.findById(verificationId)
				.orElseThrow(() -> new ValidationException("VERIFICATION_NOT_FOUND", "Verifica non trovata"));

		verification.setVerificationStatus(VerificationStatusServiceEY.ACTIVE);
		userServiceVerificationJpa.save(verification);

		// Approva tutti i documenti
		List<UserDocument> documents = userDocumentJpa.findByUserServiceVerification(verification);
		for (UserDocument doc : documents) {
			doc.setApproved(true);
			userDocumentJpa.save(doc);
		}

		// Se l'utente è in attesa, attivalo
		User user = verification.getUser();
		if (user.getStatus() == UserStatus.PENDING_ADMIN_APPROVAL) {
			user.setStatus(UserStatus.ACTIVE);
			userJpa.save(user);

			// Invia email di attivazione
			emailService.sendEmailForAccountActivation(user.getEmail(), user.getLanguage());
		}

		// Attiva anche i servizi specifici (pubblicazione)
		servicesApprovePublicationStatus(user);
	}

	@Override
	@Transactional
	public void rejectServiceVerification(Long verificationId, String motivation) {
		UserServiceVerification verification = userServiceVerificationJpa.findById(verificationId)
				.orElseThrow(() -> new ValidationException("VERIFICATION_NOT_FOUND", "Verifica non trovata"));

		User user = verification.getUser();
		String userEmail = user.getEmail();
		String userLang = user.getLanguage();

		// Pulizia dei dati correlati che impediscono la cancellazione
		cleanupUserRelatedData(user);

		userJpa.delete(user);

		// Invia email di rifiuto
		emailService.sendEmailForRejectAccount(userEmail, motivation, userLang);
	}

	private void cleanupUserRelatedData(User user) {
		// 1. Elimina OTP
		userOtpJpa.deleteByUser(user);

		// 2. Elimina Servizi e relative immagini
		List<NccServiceEntity> nccServices = nccServiceJpa.findByUser(user);
		for (NccServiceEntity s : nccServices) {
			fileService.deleteServiceImages(s.getId(), us.hogu.model.enums.ServiceType.NCC);
			nccServiceJpa.delete(s);
		}

		List<RestaurantServiceEntity> restaurantServices = restaurantServiceJpa.findByUser(user);
		for (RestaurantServiceEntity s : restaurantServices) {
			fileService.deleteServiceImages(s.getId(), us.hogu.model.enums.ServiceType.RESTAURANT);
			restaurantServiceJpa.delete(s);
		}

		List<ClubServiceEntity> clubServices = clubServiceJpa.findByUser(user);
		for (ClubServiceEntity s : clubServices) {
			fileService.deleteServiceImages(s.getId(), us.hogu.model.enums.ServiceType.CLUB);
			clubServiceJpa.delete(s);
		}

		List<LuggageServiceEntity> luggageServices = luggageServiceJpa.findByUser(user);
		for (LuggageServiceEntity s : luggageServices) {
			fileService.deleteServiceImages(s.getId(), us.hogu.model.enums.ServiceType.LUGGAGE);
			luggageServiceJpa.delete(s);
		}

		List<BnbServiceEntity> bnbServices = bnbServiceJpa.findByUser(user);
		for (BnbServiceEntity s : bnbServices) {
			fileService.deleteServiceImages(s.getId(), us.hogu.model.enums.ServiceType.BNB);
			bnbServiceJpa.delete(s);
		}
	}

	@Override
	@Transactional(readOnly = true)
	public AdminDashboardKpiResponseDto getDashboardKpis() {
		long totalUsers = userJpa.countByStatus(UserStatus.ACTIVE);
		long pendingProviders = userJpa.countByRoleAndStatus(UserRole.PROVIDER, UserStatus.PENDING_ADMIN_APPROVAL);

		List<BookingStatus> revenueStatuses = List.of(
				BookingStatus.FULL_PAYMENT_COMPLETED,
				BookingStatus.COMPLETED,
				BookingStatus.PROVIDER_LIQUIDATED,
				BookingStatus.COMMISSION_PAID,
				BookingStatus.WAITING_COMPLETION);

		java.math.BigDecimal totalRevenue = bookingJpa.calculateTotalRevenueByStatuses(revenueStatuses);

		return AdminDashboardKpiResponseDto.builder()
				.totalUsers(totalUsers)
				.pendingProviders(pendingProviders)
				.totalRevenue(totalRevenue)
				.build();
	}

	/*
	 * Approverà a tutti i servizi annessi all'utente come stato pubblicato
	 */
	private void servicesApprovePublicationStatus(User user) {
		List<NccServiceEntity> nccServices = nccServiceJpa.findByUser(user);
		if (!nccServices.isEmpty()) {
			for (NccServiceEntity nccService : nccServices) {
				nccApprovePublicationStatus(nccService);
			}
		}

		List<RestaurantServiceEntity> restaurantServices = restaurantServiceJpa.findByUser(user);
		if (!restaurantServices.isEmpty()) {
			for (RestaurantServiceEntity restaurantService : restaurantServices) {
				restaurantApprovePublicationStatus(restaurantService);
			}
		}

		List<ClubServiceEntity> clubServices = clubServiceJpa.findByUser(user);
		if (!clubServices.isEmpty()) {
			for (ClubServiceEntity clubService : clubServices) {
				clubApprovePublicationStatus(clubService);
			}
		}

		List<LuggageServiceEntity> luggageServices = luggageServiceJpa.findByUser(user);
		if (!luggageServices.isEmpty()) {
			for (LuggageServiceEntity luggageService : luggageServices) {
				luggageApprovePublicationStatus(luggageService);
			}
		}

		List<BnbServiceEntity> bnbServices = bnbServiceJpa.findByUser(user);
		if (!bnbServices.isEmpty()) {
			for (BnbServiceEntity bnbService : bnbServices) {
				bnbApprovePublicationStatus(bnbService);
			}
		}
	}

	private void nccApprovePublicationStatus(NccServiceEntity entity) {
		entity.setPublicationStatus(true);
		nccServiceJpa.save(entity);
	}

	private void restaurantApprovePublicationStatus(RestaurantServiceEntity entity) {
		entity.setPublicationStatus(true);
		restaurantServiceJpa.save(entity);
	}

	private void clubApprovePublicationStatus(ClubServiceEntity entity) {
		entity.setPublicationStatus(true);
		clubServiceJpa.save(entity);
	}

	private void luggageApprovePublicationStatus(LuggageServiceEntity entity) {
		entity.setPublicationStatus(true);
		luggageServiceJpa.save(entity);
	}

	private void bnbApprovePublicationStatus(BnbServiceEntity entity) {
		entity.setPublicationStatus(true);
		bnbServiceJpa.save(entity);
	}

	/*
	 * Sospenderà a tutti i servizi annessi all'utente come stato non pubblicato
	 */
	private void servicesSuspendPublicationStatus(User user) {
		List<NccServiceEntity> nccServices = nccServiceJpa.findByUser(user);
		if (!nccServices.isEmpty()) {
			for (NccServiceEntity nccService : nccServices) {
				nccSuspendPublicationStatus(nccService);
			}
		}

		List<RestaurantServiceEntity> restaurantServices = restaurantServiceJpa.findByUser(user);
		if (!restaurantServices.isEmpty()) {
			for (RestaurantServiceEntity restaurantService : restaurantServices) {
				restaurantSuspendPublicationStatus(restaurantService);
			}
		}

		List<ClubServiceEntity> clubServices = clubServiceJpa.findByUser(user);
		if (!clubServices.isEmpty()) {
			for (ClubServiceEntity clubService : clubServices) {
				clubSuspendPublicationStatus(clubService);
			}
		}

		List<LuggageServiceEntity> luggageServices = luggageServiceJpa.findByUser(user);
		if (!luggageServices.isEmpty()) {
			for (LuggageServiceEntity luggageService : luggageServices) {
				luggageSuspendPublicationStatus(luggageService);
			}
		}

		List<BnbServiceEntity> bnbServices = bnbServiceJpa.findByUser(user);
		if (!bnbServices.isEmpty()) {
			for (BnbServiceEntity bnbService : bnbServices) {
				bnbSuspendPublicationStatus(bnbService);
			}
		}
	}

	private void nccSuspendPublicationStatus(NccServiceEntity entity) {
		entity.setPublicationStatus(false);
		nccServiceJpa.save(entity);
	}

	private void restaurantSuspendPublicationStatus(RestaurantServiceEntity entity) {
		entity.setPublicationStatus(false);
		restaurantServiceJpa.save(entity);
	}

	private void clubSuspendPublicationStatus(ClubServiceEntity entity) {
		entity.setPublicationStatus(false);
		clubServiceJpa.save(entity);
	}

	private void luggageSuspendPublicationStatus(LuggageServiceEntity entity) {
		entity.setPublicationStatus(false);
		luggageServiceJpa.save(entity);
	}

	private void bnbSuspendPublicationStatus(BnbServiceEntity entity) {
		entity.setPublicationStatus(false);
		bnbServiceJpa.save(entity);
	}

	@Override
	@Transactional(readOnly = true)
	public Page<AdminBookingResponseDto> getBookings(String search, int page, int size) {
		Pageable pageable = PageRequest.of(page, size, org.springframework.data.domain.Sort
				.by(org.springframework.data.domain.Sort.Direction.DESC, "creationDate"));
		Page<Booking> bookingPage;

		if (search != null && !search.isEmpty()) {
			bookingPage = bookingJpa.searchByCodeOrProvider(search, pageable);
		} else {
			bookingPage = bookingJpa.findAll(pageable);
		}

		return bookingPage.map(booking -> {
			String serviceType = "UNKNOWN";
			String providerName = "N/D";
			String providerEmail = "N/D";

			if (booking instanceof us.hogu.model.NccBooking) {
				serviceType = "NCC";
				us.hogu.model.NccBooking nb = (us.hogu.model.NccBooking) booking;
				if (nb.getNccService() != null && nb.getNccService().getUser() != null) {
					providerName = nb.getNccService().getUser().getName();
					providerEmail = nb.getNccService().getUser().getEmail();
				}
			} else if (booking instanceof us.hogu.model.BnbBooking) {
				serviceType = "BNB";
				us.hogu.model.BnbBooking bb = (us.hogu.model.BnbBooking) booking;
				if (bb.getBnbService() != null && bb.getBnbService().getUser() != null) {
					providerName = bb.getBnbService().getUser().getName();
					providerEmail = bb.getBnbService().getUser().getEmail();
				}
			} else if (booking instanceof us.hogu.model.ClubBooking) {
				serviceType = "CLUB";
				us.hogu.model.ClubBooking cb = (us.hogu.model.ClubBooking) booking;
				if (cb.getClubService() != null && cb.getClubService().getUser() != null) {
					providerName = cb.getClubService().getUser().getName();
					providerEmail = cb.getClubService().getUser().getEmail();
				}
			} else if (booking instanceof us.hogu.model.RestaurantBooking) {
				serviceType = "RESTAURANT";
				us.hogu.model.RestaurantBooking rb = (us.hogu.model.RestaurantBooking) booking;
				if (rb.getRestaurantService() != null && rb.getRestaurantService().getUser() != null) {
					providerName = rb.getRestaurantService().getUser().getName();
					providerEmail = rb.getRestaurantService().getUser().getEmail();
				}
			} else if (booking instanceof us.hogu.model.LuggageBooking) {
				serviceType = "LUGGAGE";
				us.hogu.model.LuggageBooking lb = (us.hogu.model.LuggageBooking) booking;
				if (lb.getLuggageService() != null && lb.getLuggageService().getUser() != null) {
					providerName = lb.getLuggageService().getUser().getName();
					providerEmail = lb.getLuggageService().getUser().getEmail();
				}
			}

			return AdminBookingResponseDto.builder()
					.id(booking.getId())
					.bookingCode(booking.getBookingCode())
					.creationDate(booking.getCreationDate())
					.customerName(booking.getUser() != null
							? booking.getUser().getName() + " " + booking.getUser().getSurname()
							: "N/D")
					.customerEmail(booking.getUser() != null ? booking.getUser().getEmail() : "N/D")
					.totalAmount(booking.getTotalAmount())
					.status(booking.getStatus())
					.serviceType(serviceType)
					.providerName(providerName)
					.providerEmail(providerEmail)
					.statusReason(booking.getStatusReason())
					.build();
		});
	}

	@Override
	@Transactional
	public void updateBookingStatus(Long bookingId, BookingStatus status, String reason) {
		Booking booking = bookingJpa.findById(bookingId)
				.orElseThrow(() -> new ValidationException("BOOKING_NOT_FOUND", "Prenotazione non trovata"));
		BookingStatus previousStatus = booking.getStatus();

		booking.setStatus(status);
		booking.setStatusReason(reason);

		bookingJpa.save(booking);

		if (booking instanceof us.hogu.model.BnbBooking
				&& !AvailabilityBookingStatusPolicy.freeingStatuses().contains(previousStatus)
				&& AvailabilityBookingStatusPolicy.freeingStatuses().contains(status)) {
			us.hogu.model.BnbBooking bb = (us.hogu.model.BnbBooking) booking;
			if (redisAvailabilityService.markBookingReleasedOnce(bb.getId())
					&& bb.getRoom() != null
					&& bb.getRoom().getId() != null) {
				int guests = bb.getNumberOfGuests() == null ? 0 : bb.getNumberOfGuests();
				redisAvailabilityService.rollbackBnb(
						bb.getRoom().getId(),
						bb.getCheckInDate(),
						bb.getCheckOutDate(),
						guests);
			}
		}
	}

	@Override
	public AdminBookingDetailResponseDto getBookingDetail(Long bookingId) {
		Booking b = bookingJpa.findById(bookingId)
				.orElseThrow(() -> new ValidationException("BOOKING_NOT_FOUND", "Prenotazione non trovata"));

		String customerName = "";
		if (b.getBillingFirstName() != null && b.getBillingLastName() != null) {
			customerName = b.getBillingFirstName() + " " + b.getBillingLastName();
		} else if (b.getUser() != null) {
			customerName = b.getUser().getName() + " " + b.getUser().getSurname();
		}

		AdminBookingDetailResponseDto.AdminBookingDetailResponseDtoBuilder dtoBuilder = AdminBookingDetailResponseDto
				.builder()
				.id(b.getId())
				.bookingCode(b.getBookingCode())
				.status(b.getStatus())
				.statusReason(b.getStatusReason())
				.totalAmount(b.getTotalAmount())
				.creationDate(b.getCreationDate())
				.customerName(customerName)
				.customerEmail(b.getUser() != null ? b.getUser().getEmail() : null)
				.customerPhone(null);

		if (b instanceof us.hogu.model.NccBooking) {
			us.hogu.model.NccBooking nb = (us.hogu.model.NccBooking) b;
			dtoBuilder.serviceType("NCC")
					.serviceName(nb.getNccService() != null ? nb.getNccService().getName() : null)
					.providerName(nb.getNccService() != null && nb.getNccService().getUser() != null
							? nb.getNccService().getUser().getName() + " " + nb.getNccService().getUser().getSurname()
							: null)
					.pickupLocation(nb.getPickupLocation())
					.destination(nb.getDestination())
					.guests(nb.getPassengers())
					.pickupTime(nb.getPickupTime() != null ? nb.getPickupTime().toString() : null)
					.serviceDate(nb.getPickupTime() != null ? nb.getPickupTime().toLocalDate().toString() : null)
					.address(nb.getPickupLocation());

			if (nb.getNccService() != null && nb.getNccService().getImages() != null
					&& !nb.getNccService().getImages().isEmpty()) {
				dtoBuilder.serviceImage(
						"/files/ncc/" + nb.getNccService().getId() + "/" + nb.getNccService().getImages().get(0));
				dtoBuilder.additionalImages(nb.getNccService().getImages().stream()
						.map(img -> "/files/ncc/" + nb.getNccService().getId() + "/" + img)
						.collect(java.util.stream.Collectors.toList()));
			}
		} else if (b instanceof us.hogu.model.BnbBooking) {
			us.hogu.model.BnbBooking bb = (us.hogu.model.BnbBooking) b;
			dtoBuilder.serviceType("BNB")
					.serviceName(bb.getBnbService() != null ? bb.getBnbService().getName() : null)
					.providerName(bb.getBnbService() != null && bb.getBnbService().getUser() != null
							? bb.getBnbService().getUser().getName() + " " + bb.getBnbService().getUser().getSurname()
							: null)
					.serviceDate(bb.getCheckInDate() != null ? bb.getCheckInDate().toString() : null)
					.guests(bb.getNumberOfGuests())
					.address(bb.getBnbService() != null && bb.getBnbService().getLocales() != null
							&& !bb.getBnbService().getLocales().isEmpty()
									? bb.getBnbService().getLocales().get(0).getAddress()
									: null);

			if (bb.getBnbService() != null && bb.getBnbService().getImages() != null
					&& !bb.getBnbService().getImages().isEmpty()) {
				dtoBuilder.serviceImage(
						"/files/bnb/" + bb.getBnbService().getId() + "/" + bb.getBnbService().getImages().get(0));
				dtoBuilder.additionalImages(bb.getBnbService().getImages().stream()
						.map(img -> "/files/bnb/" + bb.getBnbService().getId() + "/" + img)
						.collect(java.util.stream.Collectors.toList()));
			}
		} else if (b instanceof us.hogu.model.ClubBooking) {
			us.hogu.model.ClubBooking cb = (us.hogu.model.ClubBooking) b;
			dtoBuilder.serviceType("CLUB")
					.serviceName(cb.getClubService() != null ? cb.getClubService().getName() : null)
					.providerName(cb.getClubService() != null && cb.getClubService().getUser() != null
							? cb.getClubService().getUser().getName() + " " + cb.getClubService().getUser().getSurname()
							: null)
					.serviceDate(cb.getReservationTime() != null ? cb.getReservationTime().toString() : null)
					.guests(cb.getNumberOfPeople())
					.specialRequests(cb.getSpecialRequests())
					.address(cb.getClubService() != null && cb.getClubService().getLocales() != null
							&& !cb.getClubService().getLocales().isEmpty()
									? cb.getClubService().getLocales().get(0).getAddress()
									: null);

			if (cb.getEventClubService() != null && cb.getEventClubService().getImages() != null
					&& !cb.getEventClubService().getImages().isEmpty()) {
				dtoBuilder.serviceImage("/files/club/" + cb.getClubService().getId() + "/event/"
						+ cb.getEventClubService().getId() + "/" + cb.getEventClubService().getImages().get(0));
				dtoBuilder.additionalImages(cb.getEventClubService().getImages().stream()
						.map(img -> "/files/club/" + cb.getClubService().getId() + "/event/"
								+ cb.getEventClubService().getId() + "/" + img)
						.collect(java.util.stream.Collectors.toList()));
			} else if (cb.getClubService() != null && cb.getClubService().getImages() != null
					&& !cb.getClubService().getImages().isEmpty()) {
				dtoBuilder.additionalImages(cb.getClubService().getImages().stream()
						.map(img -> "/files/club/" + cb.getClubService().getId() + "/" + img)
						.collect(java.util.stream.Collectors.toList()));
			}
		} else if (b instanceof us.hogu.model.RestaurantBooking) {
			us.hogu.model.RestaurantBooking rb = (us.hogu.model.RestaurantBooking) b;
			dtoBuilder.serviceType("RESTAURANT")
					.serviceName(rb.getRestaurantService() != null ? rb.getRestaurantService().getName() : null)
					.providerName(rb.getRestaurantService() != null && rb.getRestaurantService().getUser() != null
							? rb.getRestaurantService().getUser().getName() + " "
									+ rb.getRestaurantService().getUser().getSurname()
							: null)
					.serviceDate(rb.getReservationTime() != null ? rb.getReservationTime().toString() : null)
					.guests(rb.getNumberOfPeople())
					.specialRequests(rb.getSpecialRequests())
					.address(rb.getRestaurantService() != null && rb.getRestaurantService().getLocales() != null
							&& !rb.getRestaurantService().getLocales().isEmpty()
									? rb.getRestaurantService().getLocales().get(0).getAddress()
									: null);

			if (rb.getRestaurantService() != null && rb.getRestaurantService().getImages() != null
					&& !rb.getRestaurantService().getImages().isEmpty()) {
				dtoBuilder.serviceImage("/files/restaurant/" + rb.getRestaurantService().getId() + "/"
						+ rb.getRestaurantService().getImages().get(0));
				dtoBuilder.additionalImages(rb.getRestaurantService().getImages().stream()
						.map(img -> "/files/restaurant/" + rb.getRestaurantService().getId() + "/" + img)
						.collect(java.util.stream.Collectors.toList()));
			}
		} else if (b instanceof us.hogu.model.LuggageBooking) {
			us.hogu.model.LuggageBooking lb = (us.hogu.model.LuggageBooking) b;
			dtoBuilder.serviceType("LUGGAGE")
					.serviceName(lb.getLuggageService() != null ? lb.getLuggageService().getName() : null)
					.providerName(lb.getLuggageService() != null && lb.getLuggageService().getUser() != null
							? lb.getLuggageService().getUser().getName() + " "
									+ lb.getLuggageService().getUser().getSurname()
							: null)
					.serviceDate(lb.getPickUpTime() != null ? lb.getPickUpTime().toLocalDate().toString() : null)
					.pickupTime(lb.getPickUpTime() != null ? lb.getPickUpTime().toString() : null)
					.dropOffTime(lb.getDropOffTime() != null ? lb.getDropOffTime().toString() : null)
					.specialRequests(lb.getSpecialRequests())
					.guests((lb.getBagsSmall() != null ? lb.getBagsSmall() : 0) +
							(lb.getBagsMedium() != null ? lb.getBagsMedium() : 0) +
							(lb.getBagsLarge() != null ? lb.getBagsLarge() : 0))
					.address(lb.getLuggageService() != null && lb.getLuggageService().getLocales() != null
							&& !lb.getLuggageService().getLocales().isEmpty()
									? lb.getLuggageService().getLocales().get(0).getAddress()
									: null);

			if (lb.getLuggageService() != null && lb.getLuggageService().getImages() != null
					&& !lb.getLuggageService().getImages().isEmpty()) {
				dtoBuilder.serviceImage("/files/luggage/" + lb.getLuggageService().getId() + "/"
						+ lb.getLuggageService().getImages().get(0));
				dtoBuilder.additionalImages(lb.getLuggageService().getImages().stream()
						.map(img -> "/files/luggage/" + lb.getLuggageService().getId() + "/" + img)
						.collect(java.util.stream.Collectors.toList()));
			}
		}

		return dtoBuilder.build();
	}

	@Override
	@Transactional
	public void updateUserStatus(Long userId, UserStatus status) {
		User user = userJpa.findById(userId)
				.orElseThrow(() -> new ValidationException("USER_NOT_FOUND", "Utente non trovato"));

		user.setStatus(status);
		userJpa.save(user);

		// Svuota la cache di Redis per questo utente
		userStatusRedisService.evictUserStatus(userId);

		userServiceVerificationJpa.findByUser(user).ifPresent(verification -> {
			boolean shouldUpdateDocs = false;
			boolean docsApproved = false;

			if (status == UserStatus.ACTIVE) {
				verification.setVerificationStatus(VerificationStatusServiceEY.ACTIVE);
				shouldUpdateDocs = true;
				docsApproved = true;
				servicesApprovePublicationStatus(user);
				emailService.sendEmailForAccountActivation(user.getEmail(), user.getLanguage());
			} else if (status == UserStatus.SUSPENDED || status == UserStatus.DEACTIVATED) {
				verification.setVerificationStatus(VerificationStatusServiceEY.SUSPENDED);
				shouldUpdateDocs = true;
				docsApproved = false;
				servicesSuspendPublicationStatus(user);
				emailService.sendEmailForAccountDeactivation(user.getEmail(), user.getLanguage());
			} else if (status == UserStatus.BANNED) {
				verification.setVerificationStatus(VerificationStatusServiceEY.BANNED);
				shouldUpdateDocs = true;
				docsApproved = false;
				servicesSuspendPublicationStatus(user);
				emailService.sendEmailForAccountBanned(user.getEmail(), user.getLanguage());
			}

			if (shouldUpdateDocs) {
				userServiceVerificationJpa.save(verification);
				List<UserDocument> documents = userDocumentJpa.findByUserServiceVerification(verification);
				for (UserDocument doc : documents) {
					doc.setApproved(docsApproved);
					userDocumentJpa.save(doc);
				}
			}
		});
	}

	@Override
	@Transactional(readOnly = true)
	public AdminCustomerDetailResponseDto getCustomerDetail(Long userId) {
		User user = userJpa.findById(userId)
				.orElseThrow(() -> new ValidationException("USER_NOT_FOUND", "Utente non trovato"));

		long totalBookings = bookingJpa.countByUserId(userId);

		return AdminCustomerDetailResponseDto.builder()
				.id(user.getId())
				.name(user.getName())
				.surname(user.getSurname())
				.email(user.getEmail())
				.fiscalCode(user.getFiscalCode())
				.status(user.getStatus())
				.creationDate(user.getCreationDate())
				.lastLogin(user.getLastLogin())
				.language(user.getLanguage())
				.state(user.getState())
				.totalBookings(totalBookings)
				.build();
	}

	@Override
	@Transactional(readOnly = true)
	public List<UserDocumentResponseDto> getProviderDocuments(Long providerId) {
		User user = userJpa.findById(providerId)
				.orElseThrow(() -> new ValidationException(ErrorConstants.USER_NOT_FOUND.name(),
						ErrorConstants.USER_NOT_FOUND.getMessage()));

		List<UserDocumentResponseDto> docDtos = new ArrayList<>();
		userServiceVerificationJpa.findByUser(user).ifPresent(verification -> {
			List<UserDocumentForGetAllProjection> userDocuments = userDocumentJpa.findForGetAll(verification);
			for (UserDocumentForGetAllProjection doc : userDocuments) {
				UserDocumentResponseDto docDto = new UserDocumentResponseDto();
				docDto.setId(doc.getId());
				docDto.setFilename(doc.getFilename());
				docDto.setApproved(doc.isApproved());
				docDtos.add(docDto);
			}
		});
		return docDtos;
	}

}
