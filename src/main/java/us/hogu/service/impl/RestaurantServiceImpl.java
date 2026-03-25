package us.hogu.service.impl;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.Optional;

import javax.management.ServiceNotFoundException;

import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import us.hogu.common.constants.ErrorConstants;
import us.hogu.common.constants.CommissionConstants;
import us.hogu.controller.dto.response.InfoStatsDto;
import us.hogu.controller.dto.request.RestaurantBookingEvent;
import us.hogu.controller.dto.request.RestaurantBookingRequestDto;
import us.hogu.controller.dto.request.RestaurantServiceRequestDto;
import us.hogu.controller.dto.response.RestaurantBookingResponseDto;
import us.hogu.controller.dto.response.RestaurantManagementResponseDto;
import us.hogu.controller.dto.response.RestaurantServiceDetailResponseDto;
import us.hogu.controller.dto.response.ServiceDetailResponseDto;
import us.hogu.controller.dto.response.ServiceSummaryResponseDto;
import us.hogu.converter.BookingMapper;
import us.hogu.converter.RestaurantServiceMapper;
import us.hogu.converter.ServiceLocaleMapper;
import us.hogu.exception.UserNotFoundException;
import us.hogu.exception.ValidationException;
import us.hogu.model.RestaurantBooking;
import us.hogu.model.RestaurantServiceEntity;
import us.hogu.model.ServiceLocale;
import us.hogu.model.User;
import us.hogu.model.Payment;
import us.hogu.model.enums.BookingStatus;
import us.hogu.model.enums.PaymentMethod;
import us.hogu.model.enums.PaymentStatus;
import us.hogu.model.enums.ServiceType;
import us.hogu.repository.jpa.RestaurantBookingJpa;
import us.hogu.repository.jpa.RestaurantServiceJpa;
import us.hogu.repository.jpa.UserJpa;
import us.hogu.repository.jpa.PaymentJpa;
import us.hogu.repository.projection.RestaurantDetailProjection;
import us.hogu.repository.projection.RestaurantManagementProjection;
import us.hogu.repository.projection.RestaurantSummaryProjection;
import us.hogu.service.intefaces.FileService;
import us.hogu.service.intefaces.PayPalService;
import us.hogu.service.mq.BookingProducer;
import us.hogu.service.redis.RedisAvailabilityService;
import us.hogu.controller.dto.request.RestaurantAdvancedSearchRequestDto;
import us.hogu.controller.dto.request.RestaurantAvailabilityRequestDto;
import us.hogu.controller.dto.response.RestaurantAvailabilityResponseDto;
import us.hogu.controller.dto.response.RestaurantBookingValidationResponseDto;
import us.hogu.service.intefaces.RestaurantService;
import us.hogu.service.intefaces.StripeService;
import us.hogu.client.feign.dto.request.StripePaymentRequestDto;
import org.springframework.data.jpa.domain.Specification;

import javax.persistence.criteria.Join;
import javax.persistence.criteria.Predicate;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import us.hogu.client.feign.dto.request.PayPalPaymentRequestDto;
import us.hogu.client.feign.dto.response.PaymentResponseDto;

@RequiredArgsConstructor
@Service
public class RestaurantServiceImpl implements RestaurantService {
    private final RestaurantServiceJpa restaurantServiceRepository;
    private final RestaurantBookingJpa restaurantBookingRepository;
    private final UserJpa userRepository;
    private final PaymentJpa paymentJpa;
    private final FileService fileService;
    private final RestaurantServiceMapper restaurantServiceMapper;
    private final BookingMapper bookingMapper;
    private final ServiceLocaleMapper serviceLocaleMapper;
    private final RedisAvailabilityService redisService;
    private final BookingProducer bookingProducer;
    private final PayPalService payPalService;
    private final StripeService stripeService;

    
    // FRONTEND - lista ristoranti pubblici
    @Override
    @Transactional(readOnly = true)
    public Page<ServiceSummaryResponseDto> getActiveRestaurants(Pageable pageable) {
        String lang = LocaleContextHolder.getLocale().getLanguage();

        Page<RestaurantServiceEntity> entities = restaurantServiceRepository.findActiveByLanguage(lang, pageable);
        List<ServiceSummaryResponseDto> dtoList = new ArrayList<>();

        for (RestaurantServiceEntity entity : entities.getContent()) {
            dtoList.add(restaurantServiceMapper.toSummaryDto(entity));
        }

        return new PageImpl<>(dtoList, pageable, entities.getTotalElements());
    }
    
    // FRONTEND - dettaglio ristorante pubblico
    @Override
    @Transactional(readOnly = true)
    public ServiceDetailResponseDto getRestaurantDetail(Long restaurantId, LocalDate date, Integer numberOfPeople) {
        String language = LocaleContextHolder.getLocale().getLanguage();
        RestaurantServiceEntity entity = restaurantServiceRepository.findDetailById(restaurantId, language)
            .orElseThrow(() -> new ValidationException(
                ErrorConstants.RESTURANT_NOT_FOUND.name(),
                ErrorConstants.RESTURANT_NOT_FOUND.getMessage()
            ));

        ServiceDetailResponseDto dto = restaurantServiceMapper.toDetailDto(entity);

        // Verifica disponibilità solo se data e persone sono presenti
        if (date != null && numberOfPeople != null) {

            // Usa mezzanotte o un orario convenzionale, perché non consideri l’ora
            OffsetDateTime dateTime = date.atStartOfDay().atOffset(OffsetDateTime.now().getOffset());

            Integer bookedPeople = restaurantBookingRepository.countBookedPeopleByRestaurantAndTime(
                restaurantId,
                dateTime
            );

            int availableCapacity = entity.getCapacity() - bookedPeople;

            dto.setAvailable(availableCapacity >= numberOfPeople);

        } else {
            dto.setAvailable(true); // nessun controllo richiesto
        }

        return dto;
    }
    
    // CLIENTE - prenotazioni ristorante dell'utente
    @Override
    @Transactional(readOnly = true)
    public Page<RestaurantBookingResponseDto> getUserRestaurantBookings(Long userId, Pageable pageable) {
        Page<RestaurantBooking> bookings = restaurantBookingRepository.findByUserId(userId, pageable);
        return bookings.map(bookingMapper::toRestaurantResponseDto);
    }
    
 // FORNITORE - aggiorna ristorante esistente
    @Override
    @Transactional
    public ServiceDetailResponseDto updateRestaurant(Long providerId, Long restaurantId, 
                                                     RestaurantServiceRequestDto request, 
                                                     List<MultipartFile> images) 
    throws IOException 
    {
        // Verifica che il provider esista
        User provider = userRepository.findById(providerId)
            .orElseThrow(() -> new ValidationException(
                ErrorConstants.USER_NOT_FOUND.name(), 
                ErrorConstants.USER_NOT_FOUND.getMessage()
            ));

        // Trova il ristorante e verifica la proprietà
        RestaurantServiceEntity entity = restaurantServiceRepository.findById(restaurantId)
            .orElseThrow(() -> new ValidationException(
                ErrorConstants.RESTURANT_NOT_FOUND.name(), 
                ErrorConstants.RESTURANT_NOT_FOUND.getMessage()
            ));

        if (!entity.getUser().getId().equals(providerId)) {
            throw new ValidationException(
                ErrorConstants.RESTURANT_NOT_FOUND_OR_NOT_AUTHORIZED.name(), 
                ErrorConstants.RESTURANT_NOT_FOUND_OR_NOT_AUTHORIZED.getMessage()
            );
        }

        entity.setName(request.getName());
        entity.setDescription(request.getDescription());
        entity.setMenu(request.getMenu());
        entity.setBasePrice(request.getBasePrice());
        entity.setLocales(serviceLocaleMapper.mapRequestToEntity(request.getLocales()));
        entity.setPublicationStatus(request.getPublicationStatus());

        if (request.getCapacity() != null && !request.getCapacity().equals(entity.getCapacity())) {
            int oldCapacity = entity.getCapacity() != null ? entity.getCapacity() : 0;
            redisService.updateRestaurantCapacity(entity.getId(), oldCapacity, request.getCapacity());
        }

        entity.setCapacity(request.getCapacity());

        fileService.updateImages(entity.getId(), ServiceType.RESTAURANT, entity.getImages(), images);

        RestaurantServiceEntity updatedRestaurant = restaurantServiceRepository.save(entity);
        return restaurantServiceMapper.toDetailDto(updatedRestaurant, provider);
    }
    
    @Override
    @Transactional(readOnly = true)
    public Page<RestaurantManagementResponseDto> getProviderRestaurants(Long providerId, Pageable pageable) {
        Page<RestaurantServiceEntity> page = restaurantServiceRepository.findByProviderId(providerId, pageable);
        return page.map(restaurantServiceMapper::toManagementDto);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<RestaurantBookingResponseDto> getRestaurantBookings(Long restaurantId, Long providerId, Pageable pageable) {
        boolean restaurant = restaurantServiceRepository.existsByIdAndUserId(restaurantId, providerId);
        if (!restaurant) {
            throw new ValidationException(
                ErrorConstants.RESTURANT_NOT_FOUND_OR_NOT_AUTHORIZED.name(),
                ErrorConstants.RESTURANT_NOT_FOUND_OR_NOT_AUTHORIZED.getMessage()
            );
        }

        Page<RestaurantBooking> bookings = restaurantBookingRepository.findByRestaurantServiceId(restaurantId, pageable);
        return bookings.map(bookingMapper::toRestaurantResponseDto);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<RestaurantBookingResponseDto> getRestaurantBookingsPending(Long restaurantId, Long providerId, Pageable pageable) {
        boolean restaurant = restaurantServiceRepository.existsByIdAndUserId(restaurantId, providerId);
        if (!restaurant) {
            throw new ValidationException(
                ErrorConstants.RESTURANT_NOT_FOUND_OR_NOT_AUTHORIZED.name(),
                ErrorConstants.RESTURANT_NOT_FOUND_OR_NOT_AUTHORIZED.getMessage()
            );
        }

        Page<RestaurantBooking> bookings = restaurantBookingRepository.findPendingByRestaurantServiceId(restaurantId, pageable);
        return bookings.map(bookingMapper::toRestaurantResponseDto);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<RestaurantBookingResponseDto> getRestaurantBookingsHistory(Long restaurantId, Long providerId, Pageable pageable) {
        if (!restaurantServiceRepository.existsByIdAndUserId(restaurantId, providerId)) {
            throw new ValidationException(ErrorConstants.RESTURANT_NOT_FOUND_OR_NOT_AUTHORIZED.name(), ErrorConstants.RESTURANT_NOT_FOUND_OR_NOT_AUTHORIZED.getMessage());
        }
        // Archivio: tutto ciò che è precedente a oggi (00:00)
        OffsetDateTime today = LocalDate.now().atStartOfDay().atOffset(OffsetDateTime.now().getOffset());
        Page<RestaurantBooking> bookings = restaurantBookingRepository.findHistoryBookings(restaurantId, today, pageable);
        return bookings.map(bookingMapper::toRestaurantResponseDto);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<RestaurantBookingResponseDto> getRestaurantBookingsUpcoming(Long restaurantId, Long providerId, Pageable pageable) {
        if (!restaurantServiceRepository.existsByIdAndUserId(restaurantId, providerId)) {
            throw new ValidationException(ErrorConstants.RESTURANT_NOT_FOUND_OR_NOT_AUTHORIZED.name(), ErrorConstants.RESTURANT_NOT_FOUND_OR_NOT_AUTHORIZED.getMessage());
        }
        // In Arrivo: tutto ciò che è da oggi in poi
        OffsetDateTime today = LocalDate.now().atStartOfDay().atOffset(OffsetDateTime.now().getOffset());
        Page<RestaurantBooking> bookings = restaurantBookingRepository.findUpcomingBookings(restaurantId, today, pageable);
        return bookings.map(bookingMapper::toRestaurantResponseDto);
    }

    @Override
    @Transactional
    public void acceptBooking(Long providerId, Long bookingId) {
        RestaurantBooking booking = restaurantBookingRepository.findById(bookingId)
                .orElseThrow(() -> new ValidationException(
                        ErrorConstants.BOOKING_NOT_FOUND.name(),
                        ErrorConstants.BOOKING_NOT_FOUND.getMessage()));

        if (!booking.getRestaurantService().getUser().getId().equals(providerId)) {
            throw new ValidationException(
                    ErrorConstants.BOOKING_NOT_FOUND_OR_NOT_AUTHORIZED.name(),
                    ErrorConstants.BOOKING_NOT_FOUND_OR_NOT_AUTHORIZED.getMessage());
        }

        // Passa da PENDING a COMPLETED (o CONFIRMED se preferisci uno step intermedio)
        // Per ristoranti spesso è conferma diretta
        if (booking.getStatus() == BookingStatus.PENDING || booking.getStatus() == BookingStatus.WAITING_CUSTOMER_PAYMENT || booking.getStatus() == BookingStatus.WAITING_PROVIDER_CONFIRMATION) {
            booking.setStatus(BookingStatus.COMPLETED);
            restaurantBookingRepository.save(booking);
            
            // TODO: Invia notifica conferma al cliente
        } else {
            throw new ValidationException(
                    "INVALID_STATUS",
                    "La prenotazione non è in stato di attesa");
        }
    }

    @Override
    @Transactional
    public void cancelBooking(Long providerId, Long bookingId, String reason) {
        RestaurantBooking booking = restaurantBookingRepository.findById(bookingId)
                .orElseThrow(() -> new ValidationException(
                        ErrorConstants.BOOKING_NOT_FOUND.name(),
                        ErrorConstants.BOOKING_NOT_FOUND.getMessage()));

        if (!booking.getRestaurantService().getUser().getId().equals(providerId)) {
            throw new ValidationException(
                    ErrorConstants.BOOKING_NOT_FOUND_OR_NOT_AUTHORIZED.name(),
                    ErrorConstants.BOOKING_NOT_FOUND_OR_NOT_AUTHORIZED.getMessage());
        }

        booking.setStatus(BookingStatus.CANCELLED_BY_PROVIDER);
        booking.setStatusReason(reason);
        restaurantBookingRepository.save(booking);

        // Redis Rollback (libera i posti)
        redisService.rollbackRestaurant(
            booking.getRestaurantService().getId(), 
            booking.getReservationTime(), 
            booking.getNumberOfPeople());
            
        // TODO: Invia notifica cancellazione al cliente
    }
    
    // ADMIN - tutti i ristoranti
    @Override
    @Transactional(readOnly = true)
    public List<RestaurantManagementResponseDto> getAllRestaurantsForAdmin() 
    {
    	return restaurantServiceMapper.toManagementDtoList(restaurantServiceRepository.findAllForAdmin());
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<ServiceSummaryResponseDto> searchRestaurants(String searchTerm) {
        String lang = LocaleContextHolder.getLocale().getLanguage();

        List<RestaurantServiceEntity> entities = restaurantServiceRepository.findActiveBySearchAndLanguage(searchTerm, lang);
        List<ServiceSummaryResponseDto> dtos = new ArrayList<>();
        for (RestaurantServiceEntity entity : entities) {
            if (!redisService.checkRestaurantAvailability(entity.getId(), entity.getCapacity())) {
                continue;
            }
            ServiceSummaryResponseDto dto = restaurantServiceMapper.toSummaryDto(entity);
            dto.setAvailable(true);
            dtos.add(dto);
        }
        return dtos;
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ServiceSummaryResponseDto> advancedSearchRestaurants(RestaurantAdvancedSearchRequestDto searchRequest, Pageable pageable) {
        Specification<RestaurantServiceEntity> spec = (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();
            String lang = LocaleContextHolder.getLocale().getLanguage();
            if (searchRequest.getLocale() != null && searchRequest.getLocale().getLanguage() != null && !searchRequest.getLocale().getLanguage().isEmpty()) {
                lang = searchRequest.getLocale().getLanguage();
            }

            // Join with locales (usalo ovunque)
            Join<RestaurantServiceEntity, ServiceLocale> localesJoin = root.join("locales");

            // Publication status filter
            predicates.add(criteriaBuilder.isTrue(root.get("publicationStatus")));

            // Language filter
            predicates.add(criteriaBuilder.equal(localesJoin.get("language"), lang));

            // Location filter (Country and Province only)
            if (searchRequest.getLocale() != null) {
                if (searchRequest.getLocale().getProvince() != null && !searchRequest.getLocale().getProvince().trim().isEmpty()) {
                     predicates.add(criteriaBuilder.like(criteriaBuilder.lower(localesJoin.get("province")), "%" + searchRequest.getLocale().getProvince().trim().toLowerCase() + "%"));
                }
                if (searchRequest.getLocale().getCountry() != null && !searchRequest.getLocale().getCountry().trim().isEmpty()) {
                     predicates.add(criteriaBuilder.like(criteriaBuilder.lower(localesJoin.get("country")), "%" + searchRequest.getLocale().getCountry().trim().toLowerCase() + "%"));
                }
            }

            // Cuisine filter
            if (searchRequest.getCuisine() != null && !searchRequest.getCuisine().isBlank()) {
                predicates.add(
                    criteriaBuilder.like(
                        criteriaBuilder.lower(root.get("description")),
                        "%" + searchRequest.getCuisine().toLowerCase() + "%"
                    )
                );
            }

            // Price range
            if (searchRequest.getMinPrice() != null) {
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get("basePrice"), searchRequest.getMinPrice()));
            }
            if (searchRequest.getMaxPrice() != null) {
                predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get("basePrice"), searchRequest.getMaxPrice()));
            }

            // Capacity
            if (searchRequest.getNumberOfPeople() != null) {
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get("capacity"), searchRequest.getNumberOfPeople()));
            }

            query.distinct(true);
            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };

        Page<RestaurantServiceEntity> restaurants = restaurantServiceRepository.findAll(spec, pageable);

        // Availability filter
        if (searchRequest.getDate() != null && searchRequest.getTime() != null && searchRequest.getNumberOfPeople() != null) {

            OffsetDateTime requestedDateTime = OffsetDateTime.of(
                searchRequest.getDate(),
                searchRequest.getTime(),
                OffsetDateTime.now().getOffset()
            );

            List<ServiceSummaryResponseDto> availableRestaurants = restaurants.getContent().stream()
                .filter(restaurant -> {
                    Integer bookedPeople = restaurantBookingRepository.countBookedPeopleByRestaurantAndTime(
                        restaurant.getId(),
                        requestedDateTime
                    );
                    int availableCapacity = restaurant.getCapacity() - bookedPeople;
                    return availableCapacity >= searchRequest.getNumberOfPeople();
                })
                .map(restaurantServiceMapper::toSummaryDto)
                .collect(Collectors.toList());

            return new PageImpl<>(availableRestaurants, pageable, availableRestaurants.size());
        }

        return restaurants.map(restaurantServiceMapper::toSummaryDto);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<RestaurantBookingResponseDto> getCompletedBookingsForCommissions(Long providerId, Pageable pageable) {
        Page<RestaurantBooking> bookings = restaurantBookingRepository.findCompletedByProviderId(providerId, pageable);
        return bookings.map(bookingMapper::toRestaurantResponseDto);
    }

    @Override
    @Transactional
    public PaymentResponseDto payRestaurantCommissions(Long providerId, String returnUrl, String cancelUrl) {
        User provider = userRepository.findById(providerId)
            .orElseThrow(() -> new ValidationException(
                ErrorConstants.USER_NOT_FOUND.name(), 
                ErrorConstants.USER_NOT_FOUND.getMessage()
            ));

        List<RestaurantBooking> toSettle = restaurantBookingRepository.findCompletedByProviderIdAll(providerId);
        if (toSettle.isEmpty()) {
            throw new ValidationException("NO_COMMISSIONS", "Nessuna commissione da pagare");
        }

        BigDecimal totalCommission = toSettle.stream()
            .map(rb -> rb.getNumberOfPeople() != null ? new BigDecimal(rb.getNumberOfPeople()) : BigDecimal.ZERO)
            .map(n -> n.multiply(CommissionConstants.RESTAURANT_COMMISSION_PER_PERSON))
            .reduce(BigDecimal.ZERO, BigDecimal::add)
            .setScale(2, BigDecimal.ROUND_HALF_UP);

        PayPalPaymentRequestDto req = PayPalPaymentRequestDto.builder()
            .amount(totalCommission)
            .currency("EUR")
            .userId(providerId)
            .serviceType(ServiceType.RESTAURANT)
            .description("Commissioni ristorante Hogu")
            .returnUrl(returnUrl)
            .cancelUrl(cancelUrl)
            .build();

        PaymentResponseDto response = payPalService.createPayment(req);

        Payment payment = Payment.builder()
            .booking(null)
            .user(provider)
            .amount(totalCommission)
            .currency("EUR")
            .paymentMethod(PaymentMethod.PAYPAL)
            .paymentIdIntent(response.getPaymentIdIntent())
            .status(PaymentStatus.PENDING)
            .feeAmount(BigDecimal.ZERO)
            .netAmount(totalCommission)
            .build();
        paymentJpa.save(payment);

        return response;
    }

    @Override
    @Transactional
    public PaymentResponseDto executeRestaurantCommissionPayment(Long providerId, String paymentId, String payerId) {
        userRepository.findById(providerId)
            .orElseThrow(() -> new ValidationException(
                ErrorConstants.USER_NOT_FOUND.name(), 
                ErrorConstants.USER_NOT_FOUND.getMessage()
            ));

        PaymentResponseDto execResult = payPalService.executePayment(paymentId, payerId);

        if (execResult.getPaymentStatus() == PaymentStatus.AUTHORIZED) {
            execResult = payPalService.capturePayment(paymentId);
        }

        final PaymentStatus finalStatus = execResult.getPaymentStatus();
        Optional<Payment> opt = paymentJpa.findByPaymentIdIntent(paymentId);
        if (opt.isPresent()) {
            Payment pmt = opt.get();
            pmt.setStatus(finalStatus);
            paymentJpa.save(pmt);
        }

        if (execResult.getPaymentStatus() == PaymentStatus.COMPLETED) {
            List<RestaurantBooking> toUpdate = restaurantBookingRepository.findCompletedByProviderIdAll(providerId);
            for (RestaurantBooking rb : toUpdate) {
                rb.setStatus(BookingStatus.COMMISSION_PAID);
            }
            restaurantBookingRepository.saveAll(toUpdate);
        }

        return execResult;
    }

    
    @Override
    @Transactional(readOnly = true)
    public RestaurantAvailabilityResponseDto checkRestaurantAvailability(Long restaurantId, RestaurantAvailabilityRequestDto availabilityRequest) {
        RestaurantServiceEntity restaurant = restaurantServiceRepository.findById(restaurantId)
            .orElseThrow(() -> new ValidationException(ErrorConstants.RESTURANT_NOT_FOUND.name(), ErrorConstants.RESTURANT_NOT_FOUND.getMessage()));

        int maxCapacity = restaurant.getCapacity() != null ? restaurant.getCapacity() : 0;
        OffsetDateTime requestedTime = OffsetDateTime.of(availabilityRequest.getDate(), availabilityRequest.getTime(), OffsetDateTime.now().getOffset());

        // Check availability using Redis
        boolean isAvailable = redisService.checkRestaurant(
                restaurantId, 
                requestedTime, 
                availabilityRequest.getNumberOfPeople(), 
                maxCapacity);

        // Simulate available time slots (this would typically involve checking existing bookings)
        List<LocalTime> availableSlots = generateAvailableTimeSlots(restaurant, availabilityRequest.getDate(), availabilityRequest.getTime());

        return RestaurantAvailabilityResponseDto.builder()
            .restaurantId(restaurantId)
            .isAvailable(isAvailable)
            .availableTables(maxCapacity) // We don't have exact remaining capacity easily without another call, so returning max or just 0 if not available
            .maxCapacity(maxCapacity)
            .nextAvailableSlots(availableSlots)
            .build();
    }
    
    // CLIENTE - crea prenotazione ristorante
    @Override
    @Transactional
    public RestaurantBookingResponseDto createRestaurantBooking(RestaurantBookingRequestDto requestDto, Long userId) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new ValidationException(ErrorConstants.USER_NOT_FOUND.name(), ErrorConstants.USER_NOT_FOUND.getMessage()));
            
        RestaurantServiceEntity restaurant = restaurantServiceRepository.findById(requestDto.getRestaurantServiceId())
            .orElseThrow(() -> new ValidationException(ErrorConstants.RESTURANT_NOT_FOUND.name(), ErrorConstants.RESTURANT_NOT_FOUND.getMessage()));
            
        int maxCapacity = restaurant.getCapacity() != null ? restaurant.getCapacity() : 0;

        // Redis Reservation (High Concurrency)
        boolean reserved = redisService.reserveRestaurant(
                restaurant.getId(), 
                requestDto.getReservationTime(), 
                requestDto.getNumberOfPeople(), 
                maxCapacity);

        if (!reserved) {
            throw new ValidationException(
                    ErrorConstants.INSUFFICIENT_AVAILABLE_SEATS.name(),
                    ErrorConstants.INSUFFICIENT_AVAILABLE_SEATS.getMessage());
        }
        
        RestaurantBooking booking = bookingMapper.toRestaurantEntity(requestDto, user, restaurant);
        // Per i ristoranti, la prenotazione nasce confermata in quanto non richiede pagamento anticipato
        booking.setStatus(BookingStatus.WAITING_PROVIDER_CONFIRMATION);
        booking.setBillingFirstName(requestDto.getBillingFirstName());
        booking.setBillingLastName(requestDto.getBillingLastName());
        booking.setBillingTaxCode(requestDto.getFiscalCode());
        booking.setBillingVatNumber(requestDto.getTaxId());
        booking.setBillingAddress(requestDto.getBillingAddress());
        booking.setBillingEmail(requestDto.getBillingEmail());
        
        if (booking.getTotalAmount() == null) {
            booking.setTotalAmount(BigDecimal.ZERO);
        }
        
        RestaurantBooking savedBooking;
        try {
            savedBooking = restaurantBookingRepository.save(booking);
        } catch (Exception e) {
            redisService.rollbackRestaurant(
                restaurant.getId(), 
                requestDto.getReservationTime(), 
                requestDto.getNumberOfPeople());
            throw e;
        }
        
        // Async Event
        RestaurantBookingEvent event = RestaurantBookingEvent.builder()
                .userId(userId)
                .restaurantServiceId(restaurant.getId())
                .bookingId(savedBooking.getId())
                .reservationTime(requestDto.getReservationTime())
                .numberOfPeople(requestDto.getNumberOfPeople())
                .totalAmount(savedBooking.getTotalAmount())
                .billingFirstName(user.getName())
                .billingLastName(user.getSurname())
                .build();
        
        try {
            bookingProducer.sendRestaurantBookingRequest(event);
        } catch (Exception e) {
             // log error, booking is already saved in PENDING
        }
        
        return bookingMapper.toRestaurantResponseDto(savedBooking);
    }
    
    // FORNITORE - crea/aggiorna ristorante
    @Override
    @Transactional
    public ServiceDetailResponseDto createRestaurant(Long providerId, RestaurantServiceRequestDto requestDto,
    												 List<MultipartFile> images) 
    throws IOException
    {
        User provider = userRepository.findById(providerId)
        	.orElseThrow(() -> new ValidationException(ErrorConstants.RESTURANT_NOT_FOUND.name(), ErrorConstants.RESTURANT_NOT_FOUND.getMessage()));
            
        RestaurantServiceEntity restaurant = restaurantServiceMapper.toEntity(requestDto);
        restaurant.setUser(provider);
        restaurant.setCreationDate(OffsetDateTime.now());
        restaurant.setPublicationStatus(true);
        
        RestaurantServiceEntity savedRestaurant = restaurantServiceRepository.save(restaurant);
        savedRestaurant.setImages(new ArrayList<String>());
        
        fileService.uploadImages(savedRestaurant.getId(), ServiceType.RESTAURANT, savedRestaurant.getImages(), images);

        return restaurantServiceMapper.toDetailDto(savedRestaurant, provider);
    }
    
    // ADMIN - approva ristorante
    @Override
    @Transactional
    public void approveRestaurant(Long restaurantId) throws ServiceNotFoundException {
        RestaurantServiceEntity restaurant = restaurantServiceRepository.findById(restaurantId)
            .orElseThrow(() -> new ValidationException(ErrorConstants.RESTURANT_NOT_FOUND.name(), ErrorConstants.RESTURANT_NOT_FOUND.getMessage()));
            
        restaurant.setPublicationStatus(true);
        restaurantServiceRepository.save(restaurant);
    }
    

    private List<LocalTime> generateAvailableTimeSlots(RestaurantServiceEntity restaurant, LocalDate date, LocalTime requestedTime) {
        // This is a simplified implementation. In a real-world scenario, 
        // you would check against existing bookings
        List<LocalTime> slots = new ArrayList<>();
        LocalTime startTime = LocalTime.of(11, 0);  // Lunch start
        LocalTime endTime = LocalTime.of(22, 0);    // Dinner end

        while (startTime.isBefore(endTime)) {
            slots.add(startTime);
            startTime = startTime.plusHours(1);
        }

        return slots;
    }

    @Override
    @Transactional(readOnly = true)
    public InfoStatsDto getInfo(Long providerId) {
        InfoStatsDto infoStats = restaurantServiceRepository.getInfoStatsByProviderId(providerId);

        if (infoStats == null) {
            throw new ValidationException(
                    ErrorConstants.RESTURANT_NOT_FOUND.name(),
                    ErrorConstants.RESTURANT_NOT_FOUND.getMessage());
        }

        Long completedPeople = restaurantBookingRepository.sumCompletedPeopleByProviderId(providerId);
        if (completedPeople == null) {
            completedPeople = 0L;
        }
        BigDecimal commissionPerPerson = CommissionConstants.RESTAURANT_COMMISSION_PER_PERSON;
        infoStats.setTotalCommissionsAmount(commissionPerPerson.multiply(BigDecimal.valueOf(completedPeople)));

        return infoStats;
    }

    @Override
    @Transactional(readOnly = true)
    public RestaurantServiceDetailResponseDto getRestaurantServiceByIdAndProvider(Long serviceId, Long providerId) {
        RestaurantServiceEntity entity = restaurantServiceRepository.findDetailByIdAndProvider(serviceId, providerId)
                .orElseThrow(() -> new ValidationException(
                        ErrorConstants.RESTURANT_NOT_FOUND.name(),
                        "Ristorante non trovato o non appartiene al provider."));

        return restaurantServiceMapper.toProviderDetailDto(entity);
    }

    @Override
    @Transactional(readOnly = true)
    public RestaurantBookingValidationResponseDto validateBookingByCode(Long providerId, String code) {
        if (code == null || code.trim().isEmpty()) {
            return RestaurantBookingValidationResponseDto.builder().valid(false).build();
        }

        Optional<RestaurantBooking> opt = restaurantBookingRepository.findByBookingCodeIgnoreCase(code);
        if (opt.isEmpty()) {
            return RestaurantBookingValidationResponseDto.builder().valid(false).build();
        }
        RestaurantBooking booking = opt.get();

        if (booking.getRestaurantService() == null || booking.getRestaurantService().getUser() == null
                || !booking.getRestaurantService().getUser().getId().equals(providerId)) {
            return RestaurantBookingValidationResponseDto.builder().valid(false).bookingId(booking.getId()).build();
        }

        boolean statusOk = booking.getStatus() == BookingStatus.COMPLETED || booking.getStatus() == BookingStatus.COMMISSION_PAID;
        LocalDate today = LocalDate.now();
        LocalDate bookingDate = booking.getReservationTime() != null
                ? booking.getReservationTime().toLocalDate()
                : null;
        boolean dateOk = bookingDate != null && bookingDate.isEqual(today);

        boolean valid = statusOk && dateOk;

        String firstName = booking.getBillingFirstName() != null ? booking.getBillingFirstName()
                : (booking.getUser() != null ? booking.getUser().getName() : null);
        String lastName = booking.getBillingLastName() != null ? booking.getBillingLastName()
                : (booking.getUser() != null ? booking.getUser().getSurname() : null);
        String fullName = ((firstName != null ? firstName : "") + " " + (lastName != null ? lastName : "")).trim();

        String dateStr = booking.getReservationTime() != null
                ? booking.getReservationTime().toLocalDate().toString()
                : null;
        String timeStr = booking.getReservationTime() != null
                ? booking.getReservationTime().toLocalTime().format(DateTimeFormatter.ofPattern("HH:mm"))
                : null;

        return RestaurantBookingValidationResponseDto.builder()
                .valid(valid)
                .bookingId(booking.getId())
                .firstName(firstName)
                .lastName(lastName)
                .fullName(fullName)
                .date(dateStr)
                .time(timeStr)
                .guests(booking.getNumberOfPeople())
                .build();
    }

    @Override
    @Transactional
    public PaymentResponseDto payRestaurantCommissionsStripe(Long providerId, String returnUrl, String cancelUrl) {
        User provider = userRepository.findById(providerId)
            .orElseThrow(() -> new ValidationException(
                ErrorConstants.USER_NOT_FOUND.name(), 
                ErrorConstants.USER_NOT_FOUND.getMessage()
            ));

        List<RestaurantBooking> toSettle = restaurantBookingRepository.findCompletedByProviderIdAll(providerId);
        if (toSettle.isEmpty()) {
            throw new ValidationException("NO_COMMISSIONS", "Nessuna commissione da pagare");
        }

        BigDecimal totalCommission = toSettle.stream()
            .map(rb -> rb.getNumberOfPeople() != null ? new BigDecimal(rb.getNumberOfPeople()) : BigDecimal.ZERO)
            .map(n -> n.multiply(CommissionConstants.RESTAURANT_COMMISSION_PER_PERSON))
            .reduce(BigDecimal.ZERO, BigDecimal::add)
            .setScale(2, BigDecimal.ROUND_HALF_UP);

        StripePaymentRequestDto req = StripePaymentRequestDto.builder()
            .amount(totalCommission)
            .currency("EUR")
            .userId(providerId)
            .serviceType(ServiceType.RESTAURANT)
            .description("Commissioni ristorante Hogu (Stripe)")
            .returnUrl(returnUrl)
            .cancelUrl(cancelUrl)
            .build();

        PaymentResponseDto response = stripeService.processPayment(req);

        Payment payment = Payment.builder()
            .booking(null)
            .user(provider)
            .amount(totalCommission)
            .currency("EUR")
            .paymentMethod(PaymentMethod.STRIPE)
            .paymentIdIntent(response.getPaymentIdIntent())
            .status(PaymentStatus.PENDING)
            .feeAmount(BigDecimal.ZERO)
            .netAmount(totalCommission)
            .build();
        paymentJpa.save(payment);

        return response;
    }

    @Override
    @Transactional
    public PaymentResponseDto executeRestaurantCommissionPaymentStripe(Long providerId, String paymentId) {
        userRepository.findById(providerId)
            .orElseThrow(() -> new ValidationException(
                ErrorConstants.USER_NOT_FOUND.name(), 
                ErrorConstants.USER_NOT_FOUND.getMessage()
            ));

        PaymentResponseDto execResult = stripeService.confirmPayment(paymentId);

        if (execResult.getPaymentStatus() == PaymentStatus.AUTHORIZED) {
            execResult = stripeService.capturePayment(paymentId);
        }

        final PaymentStatus finalStatus = execResult.getPaymentStatus();
        Optional<Payment> opt = paymentJpa.findByPaymentIdIntent(paymentId);
        if (opt.isPresent()) {
            Payment pmt = opt.get();
            pmt.setStatus(finalStatus);
            paymentJpa.save(pmt);
        }

        if (execResult.getPaymentStatus() == PaymentStatus.COMPLETED) {
            List<RestaurantBooking> toUpdate = restaurantBookingRepository.findCompletedByProviderIdAll(providerId);
            for (RestaurantBooking rb : toUpdate) {
                rb.setStatus(BookingStatus.COMMISSION_PAID);
            }
            restaurantBookingRepository.saveAll(toUpdate);
        }

        return execResult;
    }
}
