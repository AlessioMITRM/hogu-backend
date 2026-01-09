package us.hogu.service.impl;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.Event;
import com.stripe.model.PaymentIntent;
import com.stripe.model.Refund;
import com.stripe.net.Webhook;

import us.hogu.repository.jpa.PaymentJpa;
import us.hogu.service.intefaces.StripeService;
import us.hogu.client.feign.dto.request.StripePaymentRequestDto;
import us.hogu.client.feign.dto.response.PaymentResponseDto;
import us.hogu.common.constants.ErrorConstants;
import us.hogu.exception.ValidationException;
import us.hogu.model.enums.PaymentStatus;

import javax.annotation.PostConstruct;
import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.Map;

@Service
@Transactional
public class StripeServiceImpl implements StripeService {

    @Value("${stripe.secret-key}")
    private String stripeSecretKey;

    @Value("${stripe.webhook-secret}")
    private String webhookSecret;

    private final PaymentJpa paymentJpa;

    public StripeServiceImpl(PaymentJpa paymentJpa) {
        this.paymentJpa = paymentJpa;
    }
    
    @PostConstruct
    public void init() {
        Stripe.apiKey = stripeSecretKey;
    }

    @Override
    public PaymentResponseDto processPayment(StripePaymentRequestDto request) {
        try {
            Map<String, Object> params = new HashMap<>();
            params.put("amount", calculateStripeAmount(request.getAmount()));
            params.put("currency", request.getCurrency().toLowerCase());
            params.put("payment_method", request.getPaymentIdMethod());
            params.put("confirm", true);
            params.put("automatic_payment_methods", Map.of("enabled", true, "allow_redirects", "never"));
            params.put("return_url", "https://yourdomain.com/payment/success");
            
            // Metadati obbligatori per tracciamento booking
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("booking_id", request.getBookingId());
            metadata.put("user_id", request.getUserId());
            metadata.put("service_type", request.getServiceType());
            params.put("metadata", metadata);
            
            if (request.getDescription() != null) {
                params.put("description", request.getDescription());
            }
            
            if (request.getCustomerEmail() != null) {
                params.put("receipt_email", request.getCustomerEmail());
            }

            PaymentIntent paymentIntent = PaymentIntent.create(params);
            return handlePaymentIntentResult(paymentIntent, request);

        } catch (StripeException e) {
            throw new ValidationException(ErrorConstants.ERROR_PAYMENT_STRIPE.name(), ErrorConstants.ERROR_PAYMENT_STRIPE.getMessage());
        }
    }

    @Override
    public PaymentResponseDto confirmPayment(String paymentIntentId) {
        try {
            PaymentIntent paymentIntent = PaymentIntent.retrieve(paymentIntentId);

            if ("succeeded".equals(paymentIntent.getStatus())) {
                return PaymentResponseDto.builder()
                    .paymentStatus(PaymentStatus.COMPLETED)
                    .paymentIdIntent(paymentIntentId)
                    .amount(paymentIntent.getAmount() / 100.0)
                    .currency(paymentIntent.getCurrency())
                    .build();
            }

            Map<String, Object> params = new HashMap<>();
            params.put("return_url", "https://yourdomain.com/payment/success");

            paymentIntent = paymentIntent.confirm(params);

            return createPaymentResponseFromIntent(paymentIntent);

        } catch (StripeException e) {
            throw new ValidationException(ErrorConstants.ERROR_PAYMENT_STRIPE.name(), ErrorConstants.ERROR_PAYMENT_STRIPE.getMessage());
        }
    }

    @Override
    public PaymentIntent createPaymentIntent(Double amount, String currency, String bookingId) {
        try {
            Map<String, Object> params = new HashMap<>();
            params.put("amount", calculateStripeAmount(amount));
            params.put("currency", currency.toLowerCase());
            params.put("automatic_payment_methods", Map.of("enabled", true));
            
            // Metadati per tracciamento
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("booking_id", bookingId);
            params.put("metadata", metadata);

            return PaymentIntent.create(params);

        } catch (StripeException e) {
            throw new ValidationException(ErrorConstants.ERROR_PAYMENT_STRIPE.name(), ErrorConstants.ERROR_PAYMENT_STRIPE.getMessage());
        }
    }

    @Override
    public boolean processRefund(String paymentIntentId, String reason) {
        try {
            Map<String, Object> params = new HashMap<>();
            params.put("payment_intent", paymentIntentId);
            params.put("reason", "requested_by_customer");

            // CORREZIONE: usa Refund invece di StripeRefund
            Refund refund = Refund.create(params);
            
            // Aggiorna lo stato del pagamento nel database
            updatePaymentStatus(paymentIntentId, PaymentStatus.FAILED);
            
            return true;

        } catch (StripeException e) {
            throw new ValidationException(ErrorConstants.ERROR_REFUND_STRIPE.name(), ErrorConstants.ERROR_REFUND_STRIPE.getMessage());
        }
    }

    @Override
    public PaymentIntent getPaymentIntent(String paymentIntentId) {
        try {
            return PaymentIntent.retrieve(paymentIntentId);
        } catch (StripeException e) {
            throw new ValidationException(ErrorConstants.PAYMENT_NOT_FOUND.name(), ErrorConstants.PAYMENT_NOT_FOUND.getMessage());
        }
    }

    @Override
    public boolean handleWebhook(String payload, String signature) {
        try {
            Event event = Webhook.constructEvent(payload, signature, webhookSecret);
            return processStripeEvent(event);
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public boolean cancelPaymentIntent(String paymentIntentId) {
        try {
            PaymentIntent paymentIntent = PaymentIntent.retrieve(paymentIntentId);
            PaymentIntent cancelledIntent = paymentIntent.cancel();
            
            updatePaymentStatus(paymentIntentId, PaymentStatus.FAILED);

            return "canceled".equals(cancelledIntent.getStatus());

        } catch (StripeException e) {
            throw new ValidationException(ErrorConstants.ERROR_PAYMENT_DELETE.name(), ErrorConstants.ERROR_PAYMENT_DELETE.getMessage());
        }
    }
    
    @Override
    public boolean refundPayment(String paymentIntentId, String reason) {
        try {
            // Recupera PaymentIntent da Stripe
            PaymentIntent paymentIntent = PaymentIntent.retrieve(paymentIntentId);

            // Controllo: il pagamento deve essere COMPLETED
            if (!"succeeded".equals(paymentIntent.getStatus())) {
                throw new ValidationException(ErrorConstants.ERROR_REFUND_STRIPE.name(), ErrorConstants.ERROR_REFUND_STRIPE.getMessage());
            }

            // Parametri per il rimborso
            Map<String, Object> params = new HashMap<>();
            params.put("payment_intent", paymentIntentId);
            if (reason != null) {
                params.put("reason", reason);
            }

            // Esegue il rimborso
            Refund refund = Refund.create(params);

            // Aggiorna stato nel database
            updatePaymentStatus(paymentIntentId, PaymentStatus.COMPLETED);

            return "succeeded".equals(refund.getStatus());

        } catch (StripeException e) {
            throw new ValidationException(
                    ErrorConstants.ERROR_REFUND_STRIPE.name(),
                    ErrorConstants.ERROR_REFUND_STRIPE.getMessage()
            );
        }
    }

    

    // METODI PRIVATI
    private PaymentResponseDto handlePaymentIntentResult(PaymentIntent paymentIntent, StripePaymentRequestDto request) {
        String status = paymentIntent.getStatus();

        switch (status) {
            case "succeeded":
                updatePaymentStatus(paymentIntent.getId(), PaymentStatus.COMPLETED);
                return PaymentResponseDto.builder()
                    .paymentStatus(PaymentStatus.COMPLETED)
                    .paymentIdIntent(paymentIntent.getId())
                    .amount(request.getAmount())
                    .currency(request.getCurrency())
                    .build();

            case "requires_action":
                return PaymentResponseDto.builder()
                    .paymentStatus(PaymentStatus.PENDING)
                    .paymentIdIntent(paymentIntent.getId())
                    .clientSecret(paymentIntent.getClientSecret())
                    .amount(request.getAmount())
                    .currency(request.getCurrency())
                    .build();

            case "requires_payment_method":
            case "canceled":
                updatePaymentStatus(paymentIntent.getId(), PaymentStatus.FAILED);
                return PaymentResponseDto.builder()
                    .paymentStatus(PaymentStatus.FAILED)
                    .paymentIdIntent(paymentIntent.getId())
                    .errorMessage("Pagamento fallito: " + status)
                    .build();

            default:
                updatePaymentStatus(paymentIntent.getId(), PaymentStatus.PENDING);
                return PaymentResponseDto.builder()
                    .paymentStatus(PaymentStatus.PENDING)
                    .paymentIdIntent(paymentIntent.getId())
                    .build();
        }
    }

    private PaymentResponseDto createPaymentResponseFromIntent(PaymentIntent paymentIntent) {
        PaymentStatus status = mapStripeStatusToPaymentStatus(paymentIntent.getStatus());
        
        return PaymentResponseDto.builder()
            .paymentStatus(status)
            .paymentIdIntent(paymentIntent.getId())
            .clientSecret(paymentIntent.getClientSecret())
            .amount(paymentIntent.getAmount() / 100.0)
            .currency(paymentIntent.getCurrency())
            .build();
    }

    private PaymentStatus mapStripeStatusToPaymentStatus(String stripeStatus) {
        switch (stripeStatus) {
            case "succeeded":
                return PaymentStatus.COMPLETED;
            case "requires_action":
            case "requires_payment_method":
                return PaymentStatus.PENDING;
            case "canceled":
            default:
                return PaymentStatus.FAILED;
        }
    }

    private boolean processStripeEvent(Event event) {
        switch (event.getType()) {
            case "payment_intent.succeeded":
                return handlePaymentSuccess(event);
            case "payment_intent.payment_failed":
                return handlePaymentFailure(event);
            case "payment_intent.canceled":
                return handlePaymentCancelled(event);
            default:
                return false;
        }
    }

    private boolean handlePaymentSuccess(Event event) {
        PaymentIntent paymentIntent = (PaymentIntent) event.getData().getObject();
        updatePaymentStatus(paymentIntent.getId(), PaymentStatus.COMPLETED);
        return true;
    }

    private boolean handlePaymentFailure(Event event) {
        PaymentIntent paymentIntent = (PaymentIntent) event.getData().getObject();
        updatePaymentStatus(paymentIntent.getId(), PaymentStatus.FAILED);
        return true;
    }

    private boolean handlePaymentCancelled(Event event) {
        PaymentIntent paymentIntent = (PaymentIntent) event.getData().getObject();
        updatePaymentStatus(paymentIntent.getId(), PaymentStatus.FAILED);
        return true;
    }

    private void updatePaymentStatus(String paymentIntentId, PaymentStatus status) {
        paymentJpa.findByPaymentIdIntent(paymentIntentId)
            .ifPresent(payment -> {
                payment.setStatus(status);
                payment.setLastUpdateDate(OffsetDateTime.now());
                paymentJpa.save(payment);
            });
    }

    private Long calculateStripeAmount(Double amount) {
        return Math.round(amount * 100);
    }
}