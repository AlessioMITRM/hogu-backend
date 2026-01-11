package us.hogu.service.impl;

import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.Event;
import com.stripe.model.PaymentIntent;
import com.stripe.model.Refund;
import com.stripe.net.Webhook;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import us.hogu.client.feign.dto.request.StripePaymentRequestDto;
import us.hogu.client.feign.dto.response.PaymentResponseDto;
import us.hogu.common.constants.ErrorConstants;
import us.hogu.exception.ValidationException;
import us.hogu.model.enums.PaymentStatus;
import us.hogu.repository.jpa.PaymentJpa;
import us.hogu.service.intefaces.StripeService;

import javax.annotation.PostConstruct;
import java.math.BigDecimal;
import java.math.RoundingMode;
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

    // Costanti per gestione monetaria sicura
    private static final BigDecimal HUNDRED = new BigDecimal("100");
    private static final int MONEY_SCALE = 2;
    private static final RoundingMode MONEY_ROUNDING = RoundingMode.HALF_UP;

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
            // Validazione importo
            BigDecimal amount = request.getAmount();
            if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
                throw new ValidationException(
                        ErrorConstants.INVALID_AMOUNT.name(),
                        ErrorConstants.INVALID_AMOUNT.getMessage());
            }

            Map<String, Object> params = new HashMap<>();

            // Conversione sicura da euro a centesimi per Stripe
            Long stripeAmount = amount
                    .multiply(HUNDRED)
                    .setScale(0, MONEY_ROUNDING)
                    .longValueExact();

            params.put("amount", stripeAmount);
            params.put("currency", request.getCurrency().toLowerCase());
            params.put("payment_method", request.getPaymentIdMethod());
            params.put("confirm", true);
            params.put("automatic_payment_methods", Map.of("enabled", true, "allow_redirects", "never"));
            params.put("return_url", "https://yourdomain.com/payment/success");

            // Metadati obbligatori per tracciamento booking
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("booking_id", request.getBookingId());
            metadata.put("user_id", request.getUserId());
            metadata.put("service_type", request.getServiceType().toString());
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
            throw new ValidationException(
                    ErrorConstants.ERROR_PAYMENT_STRIPE.name(),
                    e.getStripeError().getMessage() != null 
                        ? e.getStripeError().getMessage() 
                        : ErrorConstants.ERROR_PAYMENT_STRIPE.getMessage());
        }
    }

    @Override
    public PaymentResponseDto confirmPayment(String paymentIntentId) {
        try {
            PaymentIntent paymentIntent = PaymentIntent.retrieve(paymentIntentId);

            if ("succeeded".equals(paymentIntent.getStatus())) {
                return buildResponseFromIntent(paymentIntent);
            }

            Map<String, Object> params = new HashMap<>();
            params.put("return_url", "https://yourdomain.com/payment/success");
            paymentIntent = paymentIntent.confirm(params);

            return buildResponseFromIntent(paymentIntent);

        } catch (StripeException e) {
            throw new ValidationException(
                    ErrorConstants.ERROR_PAYMENT_STRIPE.name(),
                    ErrorConstants.ERROR_PAYMENT_STRIPE.getMessage());
        }
    }

    @Override
    public PaymentIntent createPaymentIntent(BigDecimal amount, String currency, String bookingId) {
        try {
            if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
                throw new ValidationException(
                        ErrorConstants.INVALID_AMOUNT.name(),
                        "L'importo deve essere maggiore di zero");
            }

            Map<String, Object> params = new HashMap<>();

            Long stripeAmount = amount
                    .multiply(HUNDRED)
                    .setScale(0, MONEY_ROUNDING)
                    .longValueExact();

            params.put("amount", stripeAmount);
            params.put("currency", currency.toLowerCase());
            params.put("automatic_payment_methods", Map.of("enabled", true));

            Map<String, Object> metadata = new HashMap<>();
            metadata.put("booking_id", bookingId);
            params.put("metadata", metadata);

            return PaymentIntent.create(params);

        } catch (StripeException e) {
            throw new ValidationException(
                    ErrorConstants.ERROR_PAYMENT_STRIPE.name(),
                    ErrorConstants.ERROR_PAYMENT_STRIPE.getMessage());
        }
    }

    @Override
    public boolean refundPayment(String paymentIntentId, String reason) {
        try {
            PaymentIntent paymentIntent = PaymentIntent.retrieve(paymentIntentId);

            // Controllo: il pagamento deve essere completato per poter essere rimborsato
            if (!"succeeded".equals(paymentIntent.getStatus())) {
                throw new ValidationException(
                        ErrorConstants.ERROR_REFUND_STRIPE.name(),
                        "Il pagamento non è in stato completato");
            }

            Map<String, Object> params = new HashMap<>();
            params.put("payment_intent", paymentIntentId);
            if (reason != null && !reason.trim().isEmpty()) {
                params.put("reason", reason.trim());
            }

            Refund refund = Refund.create(params);

            // Aggiorna lo stato del pagamento nel database
            updatePaymentStatus(paymentIntentId, PaymentStatus.REFUNDED);

            return "succeeded".equals(refund.getStatus());

        } catch (StripeException e) {
            throw new ValidationException(
                    ErrorConstants.ERROR_REFUND_STRIPE.name(),
                    ErrorConstants.ERROR_REFUND_STRIPE.getMessage());
        }
    }

    @Override
    public PaymentIntent getPaymentIntent(String paymentIntentId) {
        try {
            return PaymentIntent.retrieve(paymentIntentId);
        } catch (StripeException e) {
            throw new ValidationException(
                    ErrorConstants.PAYMENT_NOT_FOUND.name(),
                    ErrorConstants.PAYMENT_NOT_FOUND.getMessage());
        }
    }

    @Override
    public boolean handleWebhook(String payload, String signature) {
        try {
            Event event = Webhook.constructEvent(payload, signature, webhookSecret);
            return processStripeEvent(event);
        } catch (Exception e) {
            // In produzione: loggare l'errore
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
            throw new ValidationException(
                    ErrorConstants.ERROR_PAYMENT_DELETE.name(),
                    ErrorConstants.ERROR_PAYMENT_DELETE.getMessage());
        }
    }

    // =====================================================================
    // METODI PRIVATI
    // =====================================================================

    private PaymentResponseDto handlePaymentIntentResult(
            PaymentIntent paymentIntent,
            StripePaymentRequestDto request) {

        String status = paymentIntent.getStatus();

        if ("succeeded".equals(status)) {
            updatePaymentStatus(paymentIntent.getId(), PaymentStatus.COMPLETED);
            return PaymentResponseDto.builder()
                    .paymentStatus(PaymentStatus.COMPLETED)
                    .paymentIdIntent(paymentIntent.getId())
                    .amount(request.getAmount())  // torniamo l'importo originale in euro
                    .currency(request.getCurrency())
                    .build();
        }

        if ("requires_action".equals(status)) {
            return PaymentResponseDto.builder()
                    .paymentStatus(PaymentStatus.PENDING)
                    .paymentIdIntent(paymentIntent.getId())
                    .clientSecret(paymentIntent.getClientSecret())
                    .amount(request.getAmount())
                    .currency(request.getCurrency())
                    .build();
        }

        if ("requires_payment_method".equals(status) || "canceled".equals(status)) {
            updatePaymentStatus(paymentIntent.getId(), PaymentStatus.FAILED);
            return PaymentResponseDto.builder()
                    .paymentStatus(PaymentStatus.FAILED)
                    .paymentIdIntent(paymentIntent.getId())
                    .errorMessage("Pagamento fallito: " + status)
                    .build();
        }

        // Default: pending
        updatePaymentStatus(paymentIntent.getId(), PaymentStatus.PENDING);
        return PaymentResponseDto.builder()
                .paymentStatus(PaymentStatus.PENDING)
                .paymentIdIntent(paymentIntent.getId())
                .build();
    }

    private PaymentResponseDto buildResponseFromIntent(PaymentIntent paymentIntent) {
        PaymentStatus status = mapStripeStatusToPaymentStatus(paymentIntent.getStatus());

        // Convertiamo i centesimi ricevuti da Stripe in euro (BigDecimal)
        BigDecimal amountInEuro = new BigDecimal(paymentIntent.getAmount())
                .divide(HUNDRED, MONEY_SCALE, MONEY_ROUNDING);

        return PaymentResponseDto.builder()
                .paymentStatus(status)
                .paymentIdIntent(paymentIntent.getId())
                .clientSecret(paymentIntent.getClientSecret())
                .amount(amountInEuro)
                .currency(paymentIntent.getCurrency())
                .build();
    }

    private PaymentStatus mapStripeStatusToPaymentStatus(String stripeStatus) {
        if ("succeeded".equals(stripeStatus)) {
            return PaymentStatus.COMPLETED;
        }
        if ("requires_action".equals(stripeStatus) || "requires_payment_method".equals(stripeStatus)) {
            return PaymentStatus.PENDING;
        }
        // canceled, payment_failed, ecc...
        return PaymentStatus.FAILED;
    }

    private boolean processStripeEvent(Event event) {
        String eventType = event.getType();

        if ("payment_intent.succeeded".equals(eventType)) {
            return handlePaymentSuccess(event);
        }
        if ("payment_intent.payment_failed".equals(eventType)) {
            return handlePaymentFailure(event);
        }
        if ("payment_intent.canceled".equals(eventType)) {
            return handlePaymentCancelled(event);
        }

        // Altri eventi non gestiti
        return false;
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

	@Override
	public boolean processRefund(String paymentIntentId, String reason) {
		// TODO Auto-generated method stub
		return false;
	}
}