package us.hogu.service.impl;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import javax.transaction.Transactional;

import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import us.hogu.client.feign.dto.request.PayPalPaymentRequestDto;
import us.hogu.client.feign.dto.request.StripePaymentRequestDto;
import us.hogu.client.feign.dto.response.PaymentResponseDto;
import us.hogu.common.constants.ErrorConstants;
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

@RequiredArgsConstructor
@Service
public class PaymentServiceImpl implements PaymentService {

    private final PaymentJpa paymentJpa;
    private final RestaurantBookingJpa restaurantBookingJpa;
    private final NccBookingJpa nccBookingJpa;
    private final ClubBookingJpa clubBookingJpa;
    private final LuggageBookingJpa luggageBookingJpa;
    private final BnbBookingJpa bnbBookingJpa;
    private final UserJpa userJpa;
    private final StripeService stripeService;
    private final PayPalService payPalService;
    private final CommissionService commissionService;

    private static final BigDecimal ZERO = BigDecimal.ZERO;
    private static final int MONEY_SCALE = 2;
    private static final RoundingMode MONEY_ROUNDING = RoundingMode.HALF_UP;

    @Override
    @Transactional
    public PaymentResponseDto processStripePayment(StripePaymentRequestDto requestDto, Long userId) {
        User user = userJpa.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("Utente non trovato"));

        Booking booking = findBookingByIdAndType(requestDto.getBookingId(), requestDto.getServiceType());
        BigDecimal bookingAmount = getBookingAmount(booking, requestDto.getServiceType());

        // Verifica che la prenotazione appartenga all'utente
        if (!getBookingUserId(booking, requestDto.getServiceType()).equals(userId)) {
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

        if (stripeResult.getPaymentStatus() == PaymentStatus.COMPLETED) {
            updateBookingStatusAfterPayment(
                    requestDto.getBookingId(),
                    requestDto.getServiceType(),
                    BookingStatus.DEPOSIT_PAID);
        }

        return stripeResult;
    }

    @Override
    @Transactional
    public PaymentResponseDto processPayPalPayment(PayPalPaymentRequestDto requestDto, Long userId) {
        User user = userJpa.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("Utente non trovato"));

        Booking booking = findBookingByIdAndType(requestDto.getBookingId(), requestDto.getServiceType());
        BigDecimal bookingAmount = getBookingAmount(booking, requestDto.getServiceType());

        if (!getBookingUserId(booking, requestDto.getServiceType()).equals(userId)) {
            throw new ValidationException(
                    ErrorConstants.UNAUTHORIZED_BOOKING.name(),
                    ErrorConstants.UNAUTHORIZED_BOOKING.getMessage());
        }

        PayPalPaymentRequestDto paypalRequest = PayPalPaymentRequestDto.builder()
                .amount(bookingAmount)
                .currency("EUR")
                .bookingId(requestDto.getBookingId())
                .userId(userId)
                .serviceType(requestDto.getServiceType())
                .description("Prenotazione " + requestDto.getServiceType())
                .customerEmail(user.getEmail())
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

            if (result.getPaymentStatus() == PaymentStatus.COMPLETED) {
                ServiceType serviceType = extractServiceTypeFromBooking(payment.getBooking());
                updateBookingStatusAfterPayment(
                        payment.getBooking().getId(),
                        serviceType,
                        BookingStatus.DEPOSIT_PAID);
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
        Payment payment = paymentJpa.findByBooking_Id(bookingId)
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

    private BigDecimal getBookingAmount(Booking booking, ServiceType serviceType) {
        if (serviceType == ServiceType.RESTAURANT) {
            return ((RestaurantBooking) booking).getTotalAmount();
        }
        if (serviceType == ServiceType.NCC) {
            return ((NccBooking) booking).getTotalAmount();
        }
        if (serviceType == ServiceType.CLUB) {
            return ((ClubBooking) booking).getTotalAmount();
        }
        if (serviceType == ServiceType.LUGGAGE) {
            return ((LuggageBooking) booking).getTotalAmount();
        }
        if (serviceType == ServiceType.BNB) {
            return ((BnbBooking) booking).getTotalAmount();
        }
        throw new ValidationException(
                ErrorConstants.SERVICE_TYPE_NOT_VALID.name(),
                ErrorConstants.SERVICE_TYPE_NOT_VALID.getMessage());
    }

    private Long getBookingUserId(Booking booking, ServiceType serviceType) {
        if (serviceType == ServiceType.RESTAURANT) {
            return ((RestaurantBooking) booking).getUser().getId();
        }
        if (serviceType == ServiceType.NCC) {
            return ((NccBooking) booking).getUser().getId();
        }
        if (serviceType == ServiceType.CLUB) {
            return ((ClubBooking) booking).getUser().getId();
        }
        if (serviceType == ServiceType.LUGGAGE) {
            return ((LuggageBooking) booking).getUser().getId();
        }
        if (serviceType == ServiceType.BNB) {
            return ((BnbBooking) booking).getUser().getId();
        }
        throw new ValidationException(
                ErrorConstants.SERVICE_TYPE_NOT_VALID.name(),
                ErrorConstants.SERVICE_TYPE_NOT_VALID.getMessage());
    }

    private void updateBookingStatusAfterPayment(Long bookingId, ServiceType serviceType, BookingStatus status) {
        if (serviceType == ServiceType.RESTAURANT) {
            RestaurantBooking rb = restaurantBookingJpa.findById(bookingId)
                    .orElseThrow(() -> new ValidationException(
                            ErrorConstants.BOOKING_RESTAURANT_NOT_FOUND.name(),
                            ErrorConstants.BOOKING_RESTAURANT_NOT_FOUND.getMessage()));
            rb.setStatus(status);
            restaurantBookingJpa.save(rb);
        }
        else if (serviceType == ServiceType.NCC) {
            NccBooking nb = nccBookingJpa.findById(bookingId)
                    .orElseThrow(() -> new ValidationException(
                            ErrorConstants.BOOKING_NCC_NOT_FOUND.name(),
                            ErrorConstants.BOOKING_NCC_NOT_FOUND.getMessage()));
            nb.setStatus(status);
            nccBookingJpa.save(nb);
        }
        else if (serviceType == ServiceType.CLUB) {
            ClubBooking cb = clubBookingJpa.findById(bookingId)
                    .orElseThrow(() -> new ValidationException(
                            ErrorConstants.BOOKING_CLUB_NOT_FOUND.name(),
                            ErrorConstants.BOOKING_CLUB_NOT_FOUND.getMessage()));
            cb.setStatus(status);
            clubBookingJpa.save(cb);
        }
        else if (serviceType == ServiceType.LUGGAGE) {
            LuggageBooking lb = luggageBookingJpa.findById(bookingId)
                    .orElseThrow(() -> new ValidationException(
                            ErrorConstants.SERVICE_LUGGAGE_NOT_FOUND.name(),
                            ErrorConstants.SERVICE_LUGGAGE_NOT_FOUND.getMessage()));
            lb.setStatus(status);
            luggageBookingJpa.save(lb);
        }
        else if (serviceType == ServiceType.BNB) {
            BnbBooking bb = bnbBookingJpa.findById(bookingId)
                    .orElseThrow(() -> new ValidationException(
                            ErrorConstants.SERVICE_BNB_NOT_FOUND.name(),
                            ErrorConstants.SERVICE_BNB_NOT_FOUND.getMessage()));
            bb.setStatus(status);
            bnbBookingJpa.save(bb);
        }
        else {
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
        }
        else if (serviceType == ServiceType.NCC) {
            return nccBookingJpa.findById(bookingId)
                    .map(b -> b.getPickupTime().isBefore(now))
                    .orElse(false);
        }
        else if (serviceType == ServiceType.CLUB) {
            return clubBookingJpa.findById(bookingId)
                    .map(b -> b.getReservationTime().isBefore(now))
                    .orElse(false);
        }
        else if (serviceType == ServiceType.LUGGAGE) {
            return luggageBookingJpa.findById(bookingId)
                    .map(b -> b.getPickUpTime().isBefore(now))
                    .orElse(false);
        }
        else if (serviceType == ServiceType.BNB) {
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
}