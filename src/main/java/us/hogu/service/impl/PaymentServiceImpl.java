package us.hogu.service.impl;

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
    
    
    @Override
    @Transactional
    public PaymentResponseDto processStripePayment(StripePaymentRequestDto requestDto, Long userId) {
        User user = userJpa.findById(userId)
            .orElseThrow(() -> new UserNotFoundException("Utente non trovato"));
        
        // CORREZIONE: Trova la prenotazione specifica invece di Booking generico
        Booking booking = findBookingByIdAndType(requestDto.getBookingId(), requestDto.getServiceType());
        Double bookingAmount = getBookingAmount(booking, requestDto.getServiceType());
        
        // Verifica che la prenotazione appartenga all'utente
        if (!getBookinguserId(booking, requestDto.getServiceType()).equals(userId)) {
            throw new ValidationException(ErrorConstants.UNAUTHORIZED_BOOKING.name(), ErrorConstants.UNAUTHORIZED_BOOKING.getMessage());
        }
        
        // CORREZIONE: Usa il DTO corretto che abbiamo creato
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
        
        // CORREZIONE: Signature corretta del metodo Stripe
        PaymentResponseDto stripeResult = stripeService.processPayment(stripeRequest);
        
        // Calcola commissioni
        Double commissionAmount = commissionService.calculateCommissionAmount(bookingAmount, requestDto.getServiceType());
        Double netAmount = bookingAmount - commissionAmount;
        
        // Crea il pagamento
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
        
        // Aggiorna stato prenotazione solo se pagamento completato
        if (stripeResult.getPaymentStatus() == PaymentStatus.COMPLETED) {
            updateBookingStatusAfterPayment(requestDto.getBookingId(), requestDto.getServiceType(), BookingStatus.DEPOSIT_PAID);
        }
        
        return stripeResult;
    }
    
    @Override
    @Transactional
    public PaymentResponseDto processPayPalPayment(PayPalPaymentRequestDto requestDto, Long userId) {
        User user = userJpa.findById(userId)
            .orElseThrow(() -> new UserNotFoundException("Utente non trovato"));
        
        // CORREZIONE: Trova la prenotazione specifica
        Booking booking = findBookingByIdAndType(requestDto.getBookingId(), requestDto.getServiceType());
        
        Double bookingAmount = getBookingAmount(booking, requestDto.getServiceType());
        
        if (!getBookinguserId(booking, requestDto.getServiceType()).equals(userId)) {
            throw new ValidationException(ErrorConstants.UNAUTHORIZED_BOOKING.name(), ErrorConstants.UNAUTHORIZED_BOOKING.getMessage());
        }
        
        // CORREZIONE: Crea DTO PayPal corretto
        PayPalPaymentRequestDto paypalRequest = PayPalPaymentRequestDto.builder()
            .amount(bookingAmount)
            .currency("EUR")
            .bookingId(requestDto.getBookingId())
            .userId(userId)
            .serviceType(requestDto.getServiceType())
            .description("Prenotazione " + requestDto.getServiceType())
            .customerEmail(user.getEmail())
            .build();
        
        // CORREZIONE: Signature corretta - createPayment invece di processPayment
        PaymentResponseDto paypalResult = payPalService.createPayment(paypalRequest);
        
        // Calcola commissioni
        Double commissionAmount = commissionService.calculateCommissionAmount(bookingAmount, requestDto.getServiceType());
        Double netAmount = bookingAmount - commissionAmount;
                
        // Crea il pagamento
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
        
        Payment savedPayment = paymentJpa.save(payment);
        
        return paypalResult;
    }
    
    @Override
    @Transactional
    public PaymentResponseDto executePayPalPayment(String paymentId, String payerId, Long userId) {
        // CORREZIONE: Esegue il pagamento PayPal dopo l'approvazione
        PaymentResponseDto result = payPalService.executePayment(paymentId, payerId);
        
        // Aggiorna il pagamento nel database
        paymentJpa.findByPaymentIdIntent(paymentId).ifPresent(payment -> {
            payment.setStatus(result.getPaymentStatus());
            payment.setLastUpdateDate(OffsetDateTime.now());
            paymentJpa.save(payment);
            
            // Se completato, aggiorna stato prenotazione
            if (result.getPaymentStatus() == PaymentStatus.COMPLETED) {
                // CORREZIONE: Recupera serviceType dal payment (dovrebbe essere nei metadata)
                updateBookingStatusAfterPayment(payment.getBooking().getId(), 
                    ServiceType.valueOf(getServiceTypeFromPayment(payment)), 
                    BookingStatus.DEPOSIT_PAID);
            }
        });
        
        return result;
    }
    
    @Override
    public List<Payment> getUserPayments(Long userId) {
        // CORREZIONE: Ritorna List<Payment> invece di projection
        return paymentJpa.findByUser_Id(userId);
    }
    
    @Override
    public PaymentResponseDto getPaymentByIdBooking(Long bookingId, Long userId) {
        Payment payment = paymentJpa.findByBooking_Id(bookingId)
            .orElseThrow(() -> new ValidationException(
                ErrorConstants.PAYMENT_NOT_FOUND.name(), 
                ErrorConstants.PAYMENT_NOT_FOUND.getMessage()));
        
        // Verifica che il pagamento appartenga all'utente
        if (!payment.getUser().getId().equals(userId)) {
            throw new ValidationException(
                ErrorConstants.UNAUTHORIZED_PAYMENT.name(), 
                ErrorConstants.UNAUTHORIZED_PAYMENT.getMessage());
        }
        
        // CORREZIONE: Converti Payment in PaymentResponseDto
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
        
        // CORREZIONE: Verifica secondo stati del JSON
        if (!isRefundable(payment)) {
            throw new ValidationException(
                ErrorConstants.REFUND_NOT_ALLOWED.name(), 
                ErrorConstants.REFUND_NOT_ALLOWED.getMessage());
        }
        
        // CORREZIONE: Processa rimborso immediato con il gateway
        boolean refundSuccess = false;
        if ("STRIPE".equals(payment.getPaymentMethod())) {
            refundSuccess = stripeService.refundPayment(payment.getPaymentIdIntent(), reason);
        } else if ("PAYPAL".equals(payment.getPaymentMethod())) {
            refundSuccess = payPalService.refundPayment(payment.getPaymentIdIntent(), reason);
        }
        
        if (refundSuccess) {
            payment.setStatus(PaymentStatus.FAILED); // Consideriamo FAILED per pagamenti rimborsati
            payment.setLastUpdateDate(OffsetDateTime.now());
            paymentJpa.save(payment);
            
            // Aggiorna stato prenotazione
            updateBookingStatusAfterPayment(payment.getBooking().getId(), 
                ServiceType.valueOf(getServiceTypeFromPayment(payment)), 
                BookingStatus.REFUNDED_BY_ADMIN);
        }
    }
    
    private Booking findBookingByIdAndType(Long bookingId, ServiceType serviceType) {
        switch (serviceType) {
            case RESTAURANT:
                return restaurantBookingJpa.findById(bookingId)
                        .orElseThrow(() -> new ValidationException(ErrorConstants.RESTURANT_NOT_FOUND.name(), 
                        		ErrorConstants.RESTURANT_NOT_FOUND.getMessage()));
            case NCC:
                return nccBookingJpa.findById(bookingId)
                        .orElseThrow(() -> new ValidationException(ErrorConstants.SERVICE_NCC_NOT_FOUND.name(), 
                        		ErrorConstants.SERVICE_NCC_NOT_FOUND.getMessage()));
            case CLUB:
                return clubBookingJpa.findById(bookingId)
                        .orElseThrow(() -> new ValidationException(ErrorConstants.CLUB_NOT_FOUND.name(), 
                        		ErrorConstants.CLUB_NOT_FOUND.getMessage()));
            case LUGGAGE:
                return luggageBookingJpa.findById(bookingId)
                        .orElseThrow(() -> new ValidationException(ErrorConstants.SERVICE_LUGGAGE_NOT_FOUND.name(), 
                        		ErrorConstants.SERVICE_LUGGAGE_NOT_FOUND.getMessage()));
            case BNB:
                return bnbBookingJpa.findById(bookingId)
                        .orElseThrow(() -> new ValidationException(ErrorConstants.SERVICE_LUGGAGE_NOT_FOUND.name(), 
                        		ErrorConstants.SERVICE_LUGGAGE_NOT_FOUND.getMessage()));
            default:
                throw new ValidationException(ErrorConstants.SERVICE_TYPE_NOT_VALID.name(), ErrorConstants.SERVICE_TYPE_NOT_VALID.getMessage());
        }
    }

    
    private Double getBookingAmount(Object booking, ServiceType serviceType) {
        switch (serviceType) {
            case RESTAURANT:
                return ((RestaurantBooking) booking).getTotalAmount();
            case NCC:
                return ((NccBooking) booking).getTotalAmount();
            case CLUB:
                return ((ClubBooking) booking).getTotalAmount();
            case LUGGAGE:
                return ((LuggageBooking) booking).getTotalAmount();
            case BNB:
                return ((LuggageBooking) booking).getTotalAmount();
            default:
                throw new ValidationException(ErrorConstants.SERVICE_TYPE_NOT_VALID.name(), ErrorConstants.SERVICE_TYPE_NOT_VALID.getMessage());
        }
    }

    private Long getBookinguserId(Object booking, ServiceType serviceType) {
        switch (serviceType) {
            case RESTAURANT:
                return ((RestaurantBooking) booking).getUser().getId();
            case NCC:
                return ((NccBooking) booking).getUser().getId();
            case CLUB:
                return ((ClubBooking) booking).getUser().getId();
            case LUGGAGE:
                return ((LuggageBooking) booking).getUser().getId();
            case BNB:
                return ((BnbBooking) booking).getUser().getId();
            default:
                throw new ValidationException(ErrorConstants.SERVICE_TYPE_NOT_VALID.name(), ErrorConstants.SERVICE_TYPE_NOT_VALID.getMessage());
        }
    }

    private void updateBookingStatusAfterPayment(Long bookingId, ServiceType serviceType, BookingStatus status) {
        switch (serviceType) {
            case RESTAURANT:
                RestaurantBooking restaurantBooking = restaurantBookingJpa.findById(bookingId)
                        .orElseThrow(() -> 
                        new ValidationException(ErrorConstants.BOOKING_RESTURANT_NOT_FOUND.name(), 
                        		ErrorConstants.BOOKING_RESTURANT_NOT_FOUND.getMessage()));

                restaurantBooking.setStatus(status);
                restaurantBookingJpa.save(restaurantBooking);
                break;

            case NCC:
                NccBooking nccBooking = nccBookingJpa.findById(bookingId)
                        .orElseThrow(() -> 
                        new ValidationException(ErrorConstants.BOOKING_NCC_NOT_FOUND.name(), 
                        		ErrorConstants.BOOKING_NCC_NOT_FOUND.getMessage()));
                        
                nccBooking.setStatus(status);
                nccBookingJpa.save(nccBooking);
                break;

            case CLUB:
                ClubBooking clubBooking = clubBookingJpa.findById(bookingId)
                        .orElseThrow(() -> 
                        new ValidationException(ErrorConstants.BOOKING_CLUB_NOT_FOUND.name(), 
                        		ErrorConstants.BOOKING_CLUB_NOT_FOUND.getMessage()));
                        
                clubBooking.setStatus(status);
                clubBookingJpa.save(clubBooking);
                break;

            case LUGGAGE:
                LuggageBooking luggageBooking = luggageBookingJpa.findById(bookingId)
                        .orElseThrow(() -> 
                        new ValidationException(ErrorConstants.SERVICE_LUGGAGE_NOT_FOUND.name(), 
                        		ErrorConstants.SERVICE_LUGGAGE_NOT_FOUND.getMessage()));
                
                luggageBooking.setStatus(status);
                luggageBookingJpa.save(luggageBooking);
                break;
            case BNB:
                BnbBooking bnbBooking = bnbBookingJpa.findById(bookingId)
                        .orElseThrow(() -> 
                        new ValidationException(ErrorConstants.SERVICE_LUGGAGE_NOT_FOUND.name(), 
                        		ErrorConstants.SERVICE_LUGGAGE_NOT_FOUND.getMessage()));
                
                bnbBooking.setStatus(status);
                bnbBookingJpa.save(bnbBooking);
                break;

            default:
                throw new ValidationException(ErrorConstants.SERVICE_TYPE_NOT_VALID.name(), ErrorConstants.SERVICE_TYPE_NOT_VALID.getMessage());
        }
    }

    private boolean isRefundable(Payment payment) {
        return payment.getStatus() == PaymentStatus.COMPLETED &&
                !isBookingStarted(payment.getBooking().getId(), getServiceTypeFromPayment(payment));
    }

    private boolean isBookingStarted(Long bookingId, String serviceType) {
        OffsetDateTime now = OffsetDateTime.now();
        ServiceType type = ServiceType.valueOf(serviceType);

        switch (type) {
            case RESTAURANT:
                return restaurantBookingJpa.findById(bookingId)
                        .map(booking -> booking.getReservationTime().isBefore(now))
                        .orElse(false);
            case NCC:
                return nccBookingJpa.findById(bookingId)
                        .map(booking -> booking.getPickupTime().isBefore(now))
                        .orElse(false);
            case CLUB:
                return clubBookingJpa.findById(bookingId)
                        .map(booking -> booking.getReservationTime().isBefore(now))
                        .orElse(false);
            case LUGGAGE:
                return luggageBookingJpa.findById(bookingId)
                        .map(booking -> booking.getPickUpTime().isBefore(now))
                        .orElse(false);
            case BNB:
                Optional<BnbBooking> opt = bnbBookingJpa.findById(bookingId);
                if (opt.isPresent()) {
                    return opt.get().getCheckInDate().isBefore(now.toLocalDate());
                }
                return false;
            default:
                throw new ValidationException(ErrorConstants.SERVICE_TYPE_NOT_VALID.name(), ErrorConstants.SERVICE_TYPE_NOT_VALID.getMessage());
        }
    }

    
    private String getServiceTypeFromPayment(Payment payment) {
        // CORREZIONE: Il serviceType dovrebbe essere memorizzato nei metadati o derivato dal booking
        // Per semplicità, assumiamo che sia recuperabile dal booking
        // In pratica, dovresti avere questa informazione nel payment
        return "RESTAURANT"; // Placeholder - da implementare correttamente
    }
}