package us.hogu.service.impl;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import javax.transaction.Transactional;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import us.hogu.client.feign.dto.request.PayPalPaymentRequestDto;
import us.hogu.client.feign.dto.request.StripePaymentRequestDto;
import us.hogu.event.PaymentStatusChangedEvent;
import org.springframework.context.event.EventListener;
import us.hogu.client.feign.dto.response.PaymentResponseDto;
import us.hogu.controller.dto.response.BookingInfoDTO;
import us.hogu.common.constants.ErrorConstants;
import us.hogu.exception.ResourceNotFoundException;
import us.hogu.exception.UserNotFoundException;
import us.hogu.exception.ValidationException;
import us.hogu.model.*;
import us.hogu.model.enums.BookingStatus;
import us.hogu.model.enums.PaymentMethod;
import us.hogu.model.enums.PaymentStatus;
import us.hogu.model.enums.ServiceType;
import us.hogu.repository.jpa.*;
import us.hogu.service.intefaces.CommissionService;
import us.hogu.service.intefaces.PayPalService;
import us.hogu.service.intefaces.PaymentService;
import us.hogu.service.intefaces.StripeService;
import us.hogu.service.availability.AvailabilityBookingStatusPolicy;
import us.hogu.service.redis.RedisAvailabilityService;

@RequiredArgsConstructor
@Service
@Slf4j
public class PaymentServiceImpl implements PaymentService {

    private final PaymentJpa paymentJpa;
    private final BookingJpa bookingJpa;
    private final RestaurantBookingJpa restaurantBookingJpa;
    private final NccBookingJpa nccBookingJpa;
    private final ClubBookingJpa clubBookingJpa;
    private final LuggageBookingJpa luggageBookingJpa;
    private final BnbBookingJpa bnbBookingJpa;
    private final UserJpa userJpa;
    private final StripeService stripeService;
    private final PayPalService payPalService;
    private final CommissionService commissionService;
    private final RedisAvailabilityService redisService;

    @Value("${hogu.client.url}")
    private String clientUrl;

    private static final BigDecimal ZERO = BigDecimal.ZERO;
    private static final int MONEY_SCALE = 2;
    private static final RoundingMode MONEY_ROUNDING = RoundingMode.HALF_UP;

    @Override
    @Transactional
    public PaymentResponseDto processStripePayment(StripePaymentRequestDto requestDto, Long userId) {
        User user = userJpa.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("Utente non trovato"));

        Booking booking = findBookingByIdAndType(requestDto.getBookingId(), requestDto.getServiceType());
        BigDecimal bookingAmount = getBookingAmount(booking);

        // Verifica che la prenotazione appartenga all'utente
        if (!getBookingUserId(booking).equals(userId)) {
            throw new ValidationException(
                    ErrorConstants.UNAUTHORIZED_BOOKING.name(),
                    ErrorConstants.UNAUTHORIZED_BOOKING.getMessage());
        }

        StripePaymentRequestDto stripeRequest = StripePaymentRequestDto.builder()
                .amount(bookingAmount)
                .currency("EUR")
                .paymentIdMethod(requestDto.getPaymentIdMethod())
                .bookingId(requestDto.getBookingId())
                .userId(userId)
                .serviceType(requestDto.getServiceType())
                .description("Prenotazione " + requestDto.getServiceType())
                .customerEmail(user.getEmail())
                .returnUrl(
                        requestDto.getReturnUrl() != null ? requestDto.getReturnUrl() : clientUrl + "/payment/callback")
                .cancelUrl(
                        requestDto.getCancelUrl() != null ? requestDto.getCancelUrl() : clientUrl + "/payment/cancel")
                .build();

        PaymentResponseDto stripeResult = stripeService.processPayment(stripeRequest);

        BigDecimal commissionAmount = commissionService.calculateCommissionAmount(
                bookingAmount, requestDto.getServiceType());

        BigDecimal netAmount = bookingAmount.subtract(commissionAmount)
                .setScale(MONEY_SCALE, MONEY_ROUNDING);

        Payment payment = Payment.builder()
                .booking(booking)
                .user(user)
                .amount(bookingAmount)
                .currency("EUR")
                .paymentMethod(PaymentMethod.STRIPE)
                .paymentIdIntent(stripeResult.getPaymentIdIntent())
                .status(stripeResult.getPaymentStatus())
                .feeAmount(commissionAmount)
                .netAmount(netAmount)
                .build();

        Payment savedPayment = paymentJpa.save(payment);

        if (stripeResult.getPaymentStatus() == PaymentStatus.COMPLETED
                || stripeResult.getPaymentStatus() == PaymentStatus.AUTHORIZED) {
            updateBookingStatusAfterPayment(
                    requestDto.getBookingId(),
                    requestDto.getServiceType(),
                    stripeResult.getPaymentStatus() == PaymentStatus.AUTHORIZED
                            ? BookingStatus.PAYMENT_AUTHORIZED
                            : BookingStatus.FULL_PAYMENT_COMPLETED);
        }

        return stripeResult;
    }

    @Override
    @Transactional
    public PaymentResponseDto processPayPalPayment(PayPalPaymentRequestDto requestDto, Long userId) {
        User user = userJpa.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("Utente non trovato"));

        Booking booking = findBookingByIdAndType(requestDto.getBookingId(), requestDto.getServiceType());
        BigDecimal bookingAmount = getBookingAmount(booking);

        if (!getBookingUserId(booking).equals(userId)) {
            throw new ValidationException(
                    ErrorConstants.UNAUTHORIZED_BOOKING.name(),
                    ErrorConstants.UNAUTHORIZED_BOOKING.getMessage());
        }

        log.info("Process PayPal Payment - Incoming request URLs: Return='{}', Cancel='{}'", requestDto.getReturnUrl(),
                requestDto.getCancelUrl());

        PayPalPaymentRequestDto paypalRequest = PayPalPaymentRequestDto.builder()
                .amount(bookingAmount)
                .currency("EUR")
                .bookingId(requestDto.getBookingId())
                .userId(userId)
                .serviceType(requestDto.getServiceType())
                .description("Prenotazione " + requestDto.getServiceType())
                .customerEmail(user.getEmail())
                .returnUrl(
                        requestDto.getReturnUrl() != null ? requestDto.getReturnUrl() : clientUrl + "/payment/callback")
                .cancelUrl(
                        requestDto.getCancelUrl() != null ? requestDto.getCancelUrl() : clientUrl + "/payment/cancel")
                .build();

        PaymentResponseDto paypalResult = payPalService.createPayment(paypalRequest);

        BigDecimal commissionAmount = commissionService.calculateCommissionAmount(
                bookingAmount, requestDto.getServiceType());

        BigDecimal netAmount = bookingAmount.subtract(commissionAmount)
                .setScale(MONEY_SCALE, MONEY_ROUNDING);

        Payment payment = Payment.builder()
                .booking(booking)
                .user(user)
                .amount(bookingAmount)
                .currency("EUR")
                .paymentMethod(PaymentMethod.PAYPAL)
                .paymentIdIntent(paypalResult.getPaymentIdIntent())
                .status(paypalResult.getPaymentStatus())
                .feeAmount(commissionAmount)
                .netAmount(netAmount)
                .build();

        paymentJpa.save(payment);

        if (booking.getStatus() == BookingStatus.WAITING_CUSTOMER_PAYMENT &&
                (paypalResult.getPaymentStatus() == PaymentStatus.COMPLETED ||
                        paypalResult.getPaymentStatus() == PaymentStatus.AUTHORIZED)) {
            updateBookingStatusAfterPayment(booking.getId(), requestDto.getServiceType(),
                    BookingStatus.FULL_PAYMENT_COMPLETED);
        }

        return paypalResult;
    }

    @Override
    @Transactional
    public PaymentResponseDto executePayPalPayment(String paymentId, String payerId, Long userId) {
        PaymentResponseDto result = payPalService.executePayment(paymentId, payerId);

        Optional<Payment> optionalPayment = paymentJpa.findByPaymentIdIntent(paymentId);
        if (optionalPayment.isPresent()) {
            Payment payment = optionalPayment.get();
            payment.setStatus(result.getPaymentStatus());
            payment.setLastUpdateDate(OffsetDateTime.now());
            paymentJpa.save(payment);

            if (result.getPaymentStatus() == PaymentStatus.COMPLETED ||
                    result.getPaymentStatus() == PaymentStatus.AUTHORIZED) {

                Booking booking = payment.getBooking();
                ServiceType serviceType = extractServiceTypeFromBooking(booking);

                if (booking.getStatus() == BookingStatus.WAITING_CUSTOMER_PAYMENT) {
                    updateBookingStatusAfterPayment(booking.getId(), serviceType, BookingStatus.FULL_PAYMENT_COMPLETED);
                } else if (booking.getStatus() == BookingStatus.PENDING) {
                    // Solo se era PENDING lo portiamo a PAYMENT_AUTHORIZED
                    updateBookingStatusAfterPayment(booking.getId(), serviceType, BookingStatus.PAYMENT_AUTHORIZED);
                }
            }
        }

        return result;
    }

    @Override
    public List<Payment> getUserPayments(Long userId) {
        return paymentJpa.findByUser_Id(userId);
    }

    @Override
    public PaymentResponseDto getPaymentByIdBooking(Long bookingId, Long userId) {
        List<Payment> payments = paymentJpa.findByBooking_Id(bookingId);

        if (payments.isEmpty()) {
            throw new ValidationException(
                    ErrorConstants.PAYMENT_NOT_FOUND.name(),
                    ErrorConstants.PAYMENT_NOT_FOUND.getMessage());
        }

        // Prendi l'ultimo pagamento valido (quello con ID più alto)
        Payment payment = payments.stream()
                .max((p1, p2) -> p1.getId().compareTo(p2.getId()))
                .orElseThrow(() -> new ValidationException(
                        ErrorConstants.PAYMENT_NOT_FOUND.name(),
                        ErrorConstants.PAYMENT_NOT_FOUND.getMessage()));

        if (!payment.getUser().getId().equals(userId)) {
            throw new ValidationException(
                    ErrorConstants.UNAUTHORIZED_PAYMENT.name(),
                    ErrorConstants.UNAUTHORIZED_PAYMENT.getMessage());
        }

        return PaymentResponseDto.builder()
                .paymentStatus(payment.getStatus())
                .paymentIdIntent(payment.getPaymentIdIntent())
                .amount(payment.getAmount())
                .currency(payment.getCurrency())
                .build();
    }

    @Override
    public BookingInfoDTO getBookingInfoByPaymentId(String paymentId, Long userId) {
        Payment payment = paymentJpa.findByPaymentIdIntent(paymentId)
                .orElseThrow(() -> new ValidationException(
                        ErrorConstants.PAYMENT_NOT_FOUND.name(),
                        ErrorConstants.PAYMENT_NOT_FOUND.getMessage()));

        if (!payment.getUser().getId().equals(userId)) {
            throw new ValidationException(
                    ErrorConstants.UNAUTHORIZED_PAYMENT.name(),
                    ErrorConstants.UNAUTHORIZED_PAYMENT.getMessage());
        }

        return mapToBookingInfoDTO(payment.getBooking(), payment);
    }

    @Override
    public BookingInfoDTO getPendingBooking(Long userId) {
        Optional<Booking> bookingOpt = bookingJpa.findFirstByUserIdAndStatusOrderByCreationDateDesc(
                userId, BookingStatus.WAITING_CUSTOMER_PAYMENT);

        if (bookingOpt.isEmpty()) {
            // Fallback: controlla anche PENDING se WAITING_CUSTOMER_PAYMENT non è usato
            // ovunque
            bookingOpt = bookingJpa.findFirstByUserIdAndStatusOrderByCreationDateDesc(
                    userId, BookingStatus.PENDING);
        }

        Booking booking = bookingOpt.orElseThrow(() -> new ResourceNotFoundException(
                ErrorConstants.BOOKING_NOT_FOUND.name(),
                ErrorConstants.BOOKING_NOT_FOUND.getMessage()));

        // Cerca eventuale pagamento associato
        List<Payment> payments = paymentJpa.findByBooking_Id(booking.getId());
        // Prendi l'ultimo pagamento se presente
        Payment payment = payments.stream()
                .max((p1, p2) -> p1.getId().compareTo(p2.getId()))
                .orElse(null);

        return mapToBookingInfoDTO(booking, payment);
    }

    private BookingInfoDTO mapToBookingInfoDTO(Booking booking, Payment payment) {
        BookingInfoDTO dto = BookingInfoDTO.builder()
                .bookingId(booking.getId().toString())
                .amount(booking.getTotalAmount())
                .bookingStatus(booking.getStatus())
                .paymentStatus(payment != null ? payment.getStatus() : PaymentStatus.PENDING)
                .paymentMethod(payment != null ? payment.getPaymentMethod() : null)
                .statusReason(booking.getStatusReason())
                .build();

        if (booking instanceof RestaurantBooking) {
            RestaurantBooking rb = (RestaurantBooking) booking;
            dto.setServiceName(rb.getRestaurantService().getName());
            dto.setBookingDate(rb.getReservationTime().toString());
            dto.setProviderName(rb.getRestaurantService().getUser().getName());
            dto.setServiceType(ServiceType.RESTAURANT);
        } else if (booking instanceof ClubBooking) {
            ClubBooking cb = (ClubBooking) booking;
            dto.setServiceName(cb.getClubService().getName());
            dto.setBookingDate(cb.getReservationTime().toString());
            dto.setProviderName(cb.getClubService().getUser().getName());
            dto.setServiceType(ServiceType.CLUB);
        } else if (booking instanceof NccBooking) {
            NccBooking nb = (NccBooking) booking;
            dto.setServiceName(nb.getNccService().getName());
            dto.setBookingDate(nb.getPickupTime().toString());
            dto.setProviderName(nb.getNccService().getUser().getName());
            dto.setServiceType(ServiceType.NCC);
        } else if (booking instanceof LuggageBooking) {
            LuggageBooking lb = (LuggageBooking) booking;
            dto.setServiceName(lb.getLuggageService().getName());
            dto.setBookingDate(lb.getDropOffTime().toString());
            dto.setProviderName(lb.getLuggageService().getUser().getName());
            dto.setServiceType(ServiceType.LUGGAGE);
        } else if (booking instanceof BnbBooking) {
            BnbBooking bb = (BnbBooking) booking;
            dto.setServiceName(bb.getBnbService().getName());
            dto.setBookingDate(bb.getCheckInDate().toString());
            dto.setProviderName(bb.getBnbService().getUser().getName());
            dto.setServiceType(ServiceType.BNB);
        }
        return dto;
    }

    @Override
    @Transactional
    public void cancelBooking(Long bookingId, ServiceType serviceType, Long userId) {
        Booking booking = findBookingByIdAndType(bookingId, serviceType);

        if (!getBookingUserId(booking).equals(userId)) {
            throw new ValidationException(
                    ErrorConstants.UNAUTHORIZED_BOOKING.name(),
                    ErrorConstants.UNAUTHORIZED_BOOKING.getMessage());
        }

        deleteBooking(booking, serviceType);

        restoreCapacity(booking);
    }

    @Override
    @Transactional
    public void cancelBookingByProvider(Long bookingId, ServiceType serviceType, Long providerId, String reason) {
        Booking booking = findBookingByIdAndType(bookingId, serviceType);

        if (!isProviderOwner(booking, providerId)) {
            throw new ValidationException(
                    ErrorConstants.UNAUTHORIZED_BOOKING.name(),
                    "Il provider non è autorizzato a cancellare questa prenotazione");
        }

        // Handle Payment Void/Refund
        List<Payment> payments = paymentJpa.findByBooking_Id(booking.getId());

        for (Payment payment : payments) {
            // Se è PayPal in stato AUTHORIZED, esegui il void
            if (payment.getPaymentMethod() == PaymentMethod.PAYPAL &&
                    payment.getStatus() == PaymentStatus.AUTHORIZED) {
                try {
                    payPalService.voidPayment(payment.getPaymentIdIntent());
                    payment.setStatus(PaymentStatus.VOIDED);
                    payment.setLastUpdateDate(OffsetDateTime.now());
                    paymentJpa.save(payment);
                    log.info("PayPal payment {} voided successfully for booking {}", payment.getPaymentIdIntent(),
                            booking.getId());
                } catch (Exception e) {
                    log.error("Failed to void PayPal payment {} for booking {}", payment.getPaymentIdIntent(),
                            booking.getId(), e);
                    // Non rilanciamo l'eccezione per permettere la cancellazione della prenotazione
                }
            } else if (payment.getPaymentMethod() == PaymentMethod.STRIPE &&
                    payment.getStatus() == PaymentStatus.AUTHORIZED) {
                try {
                    stripeService.voidPayment(payment.getPaymentIdIntent());
                    payment.setStatus(PaymentStatus.VOIDED);
                    payment.setLastUpdateDate(OffsetDateTime.now());
                    paymentJpa.save(payment);
                    log.info("Stripe payment {} voided successfully for booking {}", payment.getPaymentIdIntent(),
                            booking.getId());
                } catch (Exception e) {
                    log.error("Failed to void Stripe payment {} for booking {}", payment.getPaymentIdIntent(),
                            booking.getId(), e);
                }
            }
        }

        // Aggiorna lo stato della prenotazione a CANCELLED_BY_PROVIDER e imposta la
        // motivazione
        updateBookingStatusAfterPayment(bookingId, serviceType, BookingStatus.CANCELLED_BY_PROVIDER, reason);

        restoreCapacity(booking);
    }

    @Override
    @Transactional
    public void confirmBookingByProvider(Long bookingId, ServiceType serviceType, Long providerId) {
        Booking booking = findBookingByIdAndType(bookingId, serviceType);

        if (!isProviderOwner(booking, providerId)) {
            throw new ValidationException(
                    ErrorConstants.UNAUTHORIZED_BOOKING.name(),
                    "Il provider non è autorizzato a confermare questa prenotazione");
        }

        if (serviceType == ServiceType.NCC) {
            LocalDate today = LocalDate.now();
            List<NccBooking> todaysWaiting = nccBookingJpa.findByProviderIdStatusAndDate(
                    providerId, BookingStatus.WAITING_COMPLETION, today);
            if (!todaysWaiting.isEmpty()) {
                throw new ValidationException(
                        "WAITING_COMPLETION_ALREADY_PRESENT",
                        "Esiste già una corsa da completare oggi; non puoi accettarne un'altra per oggi");
            }
            updateBookingStatusAfterPayment(bookingId, serviceType, BookingStatus.WAITING_COMPLETION);
            return;
        }

        // Cerca il pagamento associato
        List<Payment> payments = paymentJpa.findByBooking_Id(booking.getId());

        for (Payment payment : payments) {
            // Se è PayPal in stato AUTHORIZED, esegui la capture
            if (payment.getPaymentMethod() == PaymentMethod.PAYPAL &&
                    payment.getStatus() == PaymentStatus.AUTHORIZED) {
                try {
                    PaymentResponseDto captureResult = payPalService.capturePayment(payment.getPaymentIdIntent());

                    if (captureResult.getPaymentStatus() == PaymentStatus.COMPLETED) {
                        payment.setStatus(PaymentStatus.COMPLETED);
                        payment.setLastUpdateDate(OffsetDateTime.now());
                        paymentJpa.save(payment);
                        log.info("PayPal payment {} captured successfully for booking {}", payment.getPaymentIdIntent(),
                                booking.getId());
                    } else {
                        // Se fallisce, non bloccare tutto ma segnala
                        log.error("Capture failed: {}", captureResult.getErrorMessage());
                        throw new ValidationException(ErrorConstants.ERROR_PAYMENT_PAYPAL.name(),
                                "Errore durante la cattura del pagamento: " + captureResult.getErrorMessage());
                    }
                } catch (Exception e) {
                    log.error("Failed to capture PayPal payment {} for booking {}", payment.getPaymentIdIntent(),
                            booking.getId(), e);
                    throw new ValidationException(ErrorConstants.ERROR_PAYMENT_PAYPAL.name(),
                            "Impossibile confermare la prenotazione: errore nel pagamento");
                }
            } else if (payment.getPaymentMethod() == PaymentMethod.STRIPE &&
                    payment.getStatus() == PaymentStatus.AUTHORIZED) {
                try {
                    PaymentResponseDto captureResult = stripeService.capturePayment(payment.getPaymentIdIntent());

                    if (captureResult.getPaymentStatus() == PaymentStatus.COMPLETED) {
                        payment.setStatus(PaymentStatus.COMPLETED);
                        payment.setLastUpdateDate(OffsetDateTime.now());
                        paymentJpa.save(payment);
                        log.info("Stripe payment {} captured successfully for booking {}", payment.getPaymentIdIntent(),
                                booking.getId());
                    } else {
                        log.error("Capture failed: {}", captureResult.getErrorMessage());
                        throw new ValidationException(ErrorConstants.ERROR_PAYMENT_STRIPE.name(),
                                "Errore durante la cattura del pagamento: " + captureResult.getErrorMessage());
                    }
                } catch (Exception e) {
                    log.error("Failed to capture Stripe payment {} for booking {}", payment.getPaymentIdIntent(),
                            booking.getId(), e);
                    throw new ValidationException(ErrorConstants.ERROR_PAYMENT_STRIPE.name(),
                            "Impossibile confermare la prenotazione: errore nel pagamento");
                }
            }
        }

        // Aggiorna lo stato della prenotazione a FULL_PAYMENT_COMPLETED
        updateBookingStatusAfterPayment(bookingId, serviceType, BookingStatus.FULL_PAYMENT_COMPLETED);
    }

    @Override
    @Transactional
    public void completeBookingByProvider(Long bookingId, ServiceType serviceType, Long providerId) {
        Booking booking = findBookingByIdAndType(bookingId, serviceType);

        if (!isProviderOwner(booking, providerId)) {
            throw new ValidationException(
                    ErrorConstants.UNAUTHORIZED_BOOKING.name(),
                    "Il provider non è autorizzato a completare questa prenotazione");
        }

        if (serviceType == ServiceType.NCC) {
            List<Payment> payments = paymentJpa.findByBooking_Id(booking.getId());

            for (Payment payment : payments) {
                if (payment.getPaymentMethod() == PaymentMethod.PAYPAL &&
                        payment.getStatus() == PaymentStatus.AUTHORIZED) {
                    try {
                        PaymentResponseDto captureResult = payPalService.capturePayment(payment.getPaymentIdIntent());

                        if (captureResult.getPaymentStatus() == PaymentStatus.COMPLETED) {
                            payment.setStatus(PaymentStatus.COMPLETED);
                            payment.setLastUpdateDate(OffsetDateTime.now());
                            paymentJpa.save(payment);
                            log.info("PayPal payment {} captured successfully for booking {}",
                                    payment.getPaymentIdIntent(), booking.getId());
                        } else {
                            log.error("Capture failed: {}", captureResult.getErrorMessage());
                            throw new ValidationException(ErrorConstants.ERROR_PAYMENT_PAYPAL.name(),
                                    "Errore durante la cattura del pagamento: " + captureResult.getErrorMessage());
                        }
                    } catch (Exception e) {
                        log.error("Failed to capture PayPal payment {} for booking {}", payment.getPaymentIdIntent(),
                                booking.getId(), e);
                        throw new ValidationException(ErrorConstants.ERROR_PAYMENT_PAYPAL.name(),
                                "Impossibile completare la prenotazione: errore nel pagamento");
                    }
                } else if (payment.getPaymentMethod() == PaymentMethod.STRIPE &&
                        payment.getStatus() == PaymentStatus.AUTHORIZED) {
                    try {
                        PaymentResponseDto captureResult = stripeService.capturePayment(payment.getPaymentIdIntent());

                        if (captureResult.getPaymentStatus() == PaymentStatus.COMPLETED) {
                            payment.setStatus(PaymentStatus.COMPLETED);
                            payment.setLastUpdateDate(OffsetDateTime.now());
                            paymentJpa.save(payment);
                            log.info("Stripe payment {} captured successfully for booking {}",
                                    payment.getPaymentIdIntent(), booking.getId());
                        } else {
                            log.error("Capture failed: {}", captureResult.getErrorMessage());
                            throw new ValidationException(ErrorConstants.ERROR_PAYMENT_STRIPE.name(),
                                    "Errore durante la cattura del pagamento: " + captureResult.getErrorMessage());
                        }
                    } catch (Exception e) {
                        log.error("Failed to capture Stripe payment {} for booking {}", payment.getPaymentIdIntent(),
                                booking.getId(), e);
                        throw new ValidationException(ErrorConstants.ERROR_PAYMENT_STRIPE.name(),
                                "Impossibile completare la prenotazione: errore nel pagamento");
                    }
                }
            }

            updateBookingStatusAfterPayment(bookingId, serviceType, BookingStatus.COMPLETED);
            restoreCapacity(booking);
        } else {
            updateBookingStatusAfterPayment(bookingId, serviceType, BookingStatus.COMPLETED);
            restoreCapacity(booking);
        }
    }

    private boolean isProviderOwner(Booking booking, Long providerId) {
        if (booking instanceof ClubBooking) {
            return ((ClubBooking) booking).getClubService().getUser().getId().equals(providerId);
        } else if (booking instanceof RestaurantBooking) {
            return ((RestaurantBooking) booking).getRestaurantService().getUser().getId().equals(providerId);
        } else if (booking instanceof NccBooking) {
            return ((NccBooking) booking).getNccService().getUser().getId().equals(providerId);
        } else if (booking instanceof LuggageBooking) {
            return ((LuggageBooking) booking).getLuggageService().getUser().getId().equals(providerId);
        } else if (booking instanceof BnbBooking) {
            return ((BnbBooking) booking).getBnbService().getUser().getId().equals(providerId);
        }
        return false;
    }

    private void restoreCapacity(Booking booking) {
        if (booking == null || booking.getId() == null) {
            return;
        }
        if (!redisService.markBookingReleasedOnce(booking.getId())) {
            return;
        }
        if (booking instanceof ClubBooking) {
            ClubBooking cb = (ClubBooking) booking;
            redisService.rollbackEvent(cb.getEventClubService().getId(), cb.getNumberOfPeople());
        } else if (booking instanceof NccBooking) {
            NccBooking nb = (NccBooking) booking;
            redisService.rollbackNcc(nb.getNccService().getId(), nb.getPickupTime());
        } else if (booking instanceof LuggageBooking) {
            LuggageBooking lb = (LuggageBooking) booking;
            int totalBags = (lb.getBagsSmall() != null ? lb.getBagsSmall() : 0) +
                    (lb.getBagsMedium() != null ? lb.getBagsMedium() : 0) +
                    (lb.getBagsLarge() != null ? lb.getBagsLarge() : 0);
            redisService.rollbackLuggage(lb.getLuggageService().getId(), lb.getDropOffTime(), lb.getPickUpTime(),
                    totalBags);
        } else if (booking instanceof RestaurantBooking) {
            RestaurantBooking rb = (RestaurantBooking) booking;
            redisService.rollbackRestaurant(rb.getRestaurantService().getId(), rb.getReservationTime(),
                    rb.getNumberOfPeople());
        } else if (booking instanceof BnbBooking) {
            BnbBooking bb = (BnbBooking) booking;
            redisService.rollbackBnb(bb.getRoom().getId(), bb.getCheckInDate(), bb.getCheckOutDate(),
                    bb.getNumberOfGuests());
        }
    }

    private void deleteBooking(Booking booking, ServiceType serviceType) {
        // 0. Delete associated Payment first (to avoid FK violation)
        List<Payment> payments = paymentJpa.findByBooking_Id(booking.getId());

        for (Payment payment : payments) {
            // Se il pagamento è PayPal ed è in stato AUTHORIZED, esegui il void
            if (payment.getPaymentMethod() == PaymentMethod.PAYPAL &&
                    payment.getStatus() == PaymentStatus.AUTHORIZED) {
                try {
                    payPalService.voidPayment(payment.getPaymentIdIntent());
                    log.info("PayPal payment {} voided successfully for booking {}", payment.getPaymentIdIntent(),
                            booking.getId());
                } catch (Exception e) {
                    log.error("Failed to void PayPal payment {} for booking {}", payment.getPaymentIdIntent(),
                            booking.getId(), e);
                    // Non rilanciamo l'eccezione per permettere la cancellazione della prenotazione
                }
            } else if (payment.getPaymentMethod() == PaymentMethod.STRIPE &&
                    payment.getStatus() == PaymentStatus.AUTHORIZED) {
                try {
                    stripeService.voidPayment(payment.getPaymentIdIntent());
                    log.info("Stripe payment {} voided successfully for booking {}", payment.getPaymentIdIntent(),
                            booking.getId());
                } catch (Exception e) {
                    log.error("Failed to void Stripe payment {} for booking {}", payment.getPaymentIdIntent(),
                            booking.getId(), e);
                }
            }
        }

        if (!payments.isEmpty()) {
            paymentJpa.deleteAll(payments);
        }

        switch (serviceType) {
            case CLUB:
                clubBookingJpa.delete((ClubBooking) booking);
                break;
            case NCC:
                nccBookingJpa.delete((NccBooking) booking);
                break;
            case LUGGAGE:
                luggageBookingJpa.delete((LuggageBooking) booking);
                break;
            case RESTAURANT:
                restaurantBookingJpa.delete((RestaurantBooking) booking);
                break;
            case BNB:
                bnbBookingJpa.delete((BnbBooking) booking);
                break;
            default:
                throw new IllegalArgumentException("Tipo di servizio non supportato per la cancellazione");
        }
    }

    @Override
    public void requestRefund(Long paymentId, Long userId, String reason) {
        Payment payment = paymentJpa.findById(paymentId)
                .orElseThrow(() -> new ValidationException(
                        ErrorConstants.PAYMENT_NOT_FOUND.name(),
                        ErrorConstants.PAYMENT_NOT_FOUND.getMessage()));

        if (!payment.getUser().getId().equals(userId)) {
            throw new ValidationException(
                    ErrorConstants.UNAUTHORIZED_PAYMENT.name(),
                    ErrorConstants.UNAUTHORIZED_PAYMENT.getMessage());
        }

        if (!isRefundable(payment)) {
            throw new ValidationException(
                    ErrorConstants.REFUND_NOT_ALLOWED.name(),
                    ErrorConstants.REFUND_NOT_ALLOWED.getMessage());
        }

        boolean refundSuccess = false;
        if (PaymentMethod.STRIPE.equals(payment.getPaymentMethod())) {
            refundSuccess = stripeService.refundPayment(payment.getPaymentIdIntent(), reason);
        } else if (PaymentMethod.PAYPAL.equals(payment.getPaymentMethod())) {
            refundSuccess = payPalService.refundPayment(payment.getPaymentIdIntent(), reason);
        }

        if (refundSuccess) {
            payment.setStatus(PaymentStatus.FAILED); // oppure crea un nuovo enum REFUNDED se preferisci
            payment.setLastUpdateDate(OffsetDateTime.now());
            paymentJpa.save(payment);

            ServiceType serviceType = extractServiceTypeFromBooking(payment.getBooking());
            updateBookingStatusAfterPayment(
                    payment.getBooking().getId(),
                    serviceType,
                    BookingStatus.REFUNDED_BY_ADMIN);
            if (payment.getBooking() instanceof BnbBooking) {
                restoreCapacity(payment.getBooking());
            }
        }
    }

    // ========================================================================
    // Metodi privati di supporto - Java 11 compatibili
    // ========================================================================

    private Booking findBookingByIdAndType(Long bookingId, ServiceType serviceType) {
        if (serviceType == ServiceType.RESTAURANT) {
            return restaurantBookingJpa.findById(bookingId)
                    .orElseThrow(() -> new ValidationException(
                            ErrorConstants.RESTAURANT_NOT_FOUND.name(),
                            ErrorConstants.RESTAURANT_NOT_FOUND.getMessage()));
        }
        if (serviceType == ServiceType.NCC) {
            return nccBookingJpa.findById(bookingId)
                    .orElseThrow(() -> new ValidationException(
                            ErrorConstants.SERVICE_NCC_NOT_FOUND.name(),
                            ErrorConstants.SERVICE_NCC_NOT_FOUND.getMessage()));
        }
        if (serviceType == ServiceType.CLUB) {
            return clubBookingJpa.findById(bookingId)
                    .orElseThrow(() -> new ValidationException(
                            ErrorConstants.CLUB_NOT_FOUND.name(),
                            ErrorConstants.CLUB_NOT_FOUND.getMessage()));
        }
        if (serviceType == ServiceType.LUGGAGE) {
            return luggageBookingJpa.findById(bookingId)
                    .orElseThrow(() -> new ValidationException(
                            ErrorConstants.SERVICE_LUGGAGE_NOT_FOUND.name(),
                            ErrorConstants.SERVICE_LUGGAGE_NOT_FOUND.getMessage()));
        }
        if (serviceType == ServiceType.BNB) {
            return bnbBookingJpa.findById(bookingId)
                    .orElseThrow(() -> new ValidationException(
                            ErrorConstants.SERVICE_BNB_NOT_FOUND.name(),
                            ErrorConstants.SERVICE_BNB_NOT_FOUND.getMessage()));
        }
        throw new ValidationException(
                ErrorConstants.SERVICE_TYPE_NOT_VALID.name(),
                ErrorConstants.SERVICE_TYPE_NOT_VALID.getMessage());
    }

    private BigDecimal getBookingAmount(Booking booking) {
        return booking.getTotalAmount();
    }

    private Long getBookingUserId(Booking booking) {
        return booking.getUser().getId();
    }

    private void updateBookingStatusAfterPayment(Long bookingId, ServiceType serviceType, BookingStatus status) {
        updateBookingStatusAfterPayment(bookingId, serviceType, status, null);
    }

    private void updateBookingStatusAfterPayment(Long bookingId, ServiceType serviceType, BookingStatus status,
            String reason) {
        if (serviceType == ServiceType.RESTAURANT) {
            RestaurantBooking rb = restaurantBookingJpa.findById(bookingId)
                    .orElseThrow(() -> new ValidationException(
                            ErrorConstants.BOOKING_RESTAURANT_NOT_FOUND.name(),
                            ErrorConstants.BOOKING_RESTAURANT_NOT_FOUND.getMessage()));

            rb.setStatus(status);

            if (reason != null)
                rb.setStatusReason(reason);

            restaurantBookingJpa.save(rb);
        } else if (serviceType == ServiceType.NCC) {
            NccBooking nb = nccBookingJpa.findById(bookingId)
                    .orElseThrow(() -> new ValidationException(
                            ErrorConstants.BOOKING_NCC_NOT_FOUND.name(),
                            ErrorConstants.BOOKING_NCC_NOT_FOUND.getMessage()));
            nb.setStatus(status);

            if (reason != null)
                nb.setStatusReason(reason);

            nccBookingJpa.save(nb);
        } else if (serviceType == ServiceType.CLUB) {
            ClubBooking cb = clubBookingJpa.findById(bookingId)
                    .orElseThrow(() -> new ValidationException(
                            ErrorConstants.BOOKING_CLUB_NOT_FOUND.name(),
                            ErrorConstants.BOOKING_CLUB_NOT_FOUND.getMessage()));
            cb.setStatus(status);

            if (reason != null)
                cb.setStatusReason(reason);

            clubBookingJpa.save(cb);
        } else if (serviceType == ServiceType.LUGGAGE) {
            LuggageBooking lb = luggageBookingJpa.findById(bookingId)
                    .orElseThrow(() -> new ValidationException(
                            ErrorConstants.SERVICE_LUGGAGE_NOT_FOUND.name(),
                            ErrorConstants.SERVICE_LUGGAGE_NOT_FOUND.getMessage()));
            lb.setStatus(status);

            if (reason != null)
                lb.setStatusReason(reason);

            luggageBookingJpa.save(lb);
        } else if (serviceType == ServiceType.BNB) {
            BnbBooking bb = bnbBookingJpa.findById(bookingId)
                    .orElseThrow(() -> new ValidationException(
                            ErrorConstants.SERVICE_BNB_NOT_FOUND.name(),
                            ErrorConstants.SERVICE_BNB_NOT_FOUND.getMessage()));

            bb.setStatus(status);

            if (reason != null)
                bb.setStatusReason(reason);

            bnbBookingJpa.save(bb);
        } else {
            throw new ValidationException(
                    ErrorConstants.SERVICE_TYPE_NOT_VALID.name(),
                    ErrorConstants.SERVICE_TYPE_NOT_VALID.getMessage());
        }
    }

    private boolean isRefundable(Payment payment) {
        return payment.getStatus() == PaymentStatus.COMPLETED &&
                !isBookingStarted(payment.getBooking().getId(), extractServiceTypeFromBooking(payment.getBooking()));
    }

    private boolean isBookingStarted(Long bookingId, ServiceType serviceType) {
        OffsetDateTime now = OffsetDateTime.now();

        if (serviceType == ServiceType.RESTAURANT) {
            return restaurantBookingJpa.findById(bookingId)
                    .map(b -> b.getReservationTime().isBefore(now))
                    .orElse(false);
        } else if (serviceType == ServiceType.NCC) {
            return nccBookingJpa.findById(bookingId)
                    .map(b -> b.getPickupTime().isBefore(now))
                    .orElse(false);
        } else if (serviceType == ServiceType.CLUB) {
            return clubBookingJpa.findById(bookingId)
                    .map(b -> b.getReservationTime().isBefore(now))
                    .orElse(false);
        } else if (serviceType == ServiceType.LUGGAGE) {
            return luggageBookingJpa.findById(bookingId)
                    .map(b -> b.getPickUpTime().isBefore(now))
                    .orElse(false);
        } else if (serviceType == ServiceType.BNB) {
            return bnbBookingJpa.findById(bookingId)
                    .map(b -> b.getCheckInDate().isBefore(now.toLocalDate()))
                    .orElse(false);
        }
        return false;
    }

    private ServiceType extractServiceTypeFromBooking(Booking booking) {
        if (booking instanceof RestaurantBooking) {
            return ServiceType.RESTAURANT;
        }
        if (booking instanceof NccBooking) {
            return ServiceType.NCC;
        }
        if (booking instanceof ClubBooking) {
            return ServiceType.CLUB;
        }
        if (booking instanceof LuggageBooking) {
            return ServiceType.LUGGAGE;
        }
        if (booking instanceof BnbBooking) {
            return ServiceType.BNB;
        }
        throw new IllegalStateException("Tipo di booking non riconosciuto");
    }

    @EventListener
    @Transactional
    public void handlePaymentStatusChange(PaymentStatusChangedEvent event) {
        Payment payment = event.getPayment();
        Booking booking = payment.getBooking();
        if (booking == null)
            return;

        ServiceType serviceType = extractServiceTypeFromBooking(booking);

        if (payment.getStatus() == PaymentStatus.COMPLETED || payment.getStatus() == PaymentStatus.AUTHORIZED) {
            if (booking.getStatus() == BookingStatus.WAITING_CUSTOMER_PAYMENT
                    || booking.getStatus() == BookingStatus.PENDING) {
                if (payment.getStatus() == PaymentStatus.AUTHORIZED) {
                    updateBookingStatusAfterPayment(booking.getId(), serviceType, BookingStatus.PAYMENT_AUTHORIZED);
                } else {
                    updateBookingStatusAfterPayment(booking.getId(), serviceType, BookingStatus.FULL_PAYMENT_COMPLETED);
                }
            }
            return;
        }

        if (!(booking instanceof BnbBooking)) {
            return;
        }

        if (payment.getStatus() == PaymentStatus.FAILED || payment.getStatus() == PaymentStatus.VOIDED) {
            if (!AvailabilityBookingStatusPolicy.freeingStatuses().contains(booking.getStatus())) {
                updateBookingStatusAfterPayment(
                        booking.getId(),
                        serviceType,
                        BookingStatus.CANCELLED_BY_ADMIN,
                        "Pagamento non completato");
            }
            restoreCapacity(booking);
            return;
        }

        if (payment.getStatus() == PaymentStatus.REFUNDED) {
            if (booking.getStatus() != BookingStatus.REFUNDED_BY_ADMIN) {
                updateBookingStatusAfterPayment(booking.getId(), serviceType, BookingStatus.REFUNDED_BY_ADMIN);
            }
            restoreCapacity(booking);
        }
    }
}
