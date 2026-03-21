package us.hogu.service.impl;

import com.google.gson.Gson;
import com.stripe.Stripe;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.exception.StripeException;
import com.stripe.model.Event;
import com.stripe.model.EventDataObjectDeserializer;
import com.stripe.model.PaymentIntent;
import com.stripe.model.Refund;
import com.stripe.model.StripeObject;
import com.stripe.model.checkout.Session;
import com.stripe.param.checkout.SessionCreateParams;
import com.stripe.net.Webhook;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import us.hogu.client.feign.dto.request.StripePaymentRequestDto;
import us.hogu.client.feign.dto.response.PaymentResponseDto;
import us.hogu.common.constants.ErrorConstants;
import us.hogu.exception.ValidationException;
import us.hogu.model.enums.PaymentMethod;
import us.hogu.model.enums.PaymentStatus;
import us.hogu.model.Payment;
import us.hogu.repository.jpa.PaymentJpa;
import us.hogu.service.intefaces.StripeService;

import javax.annotation.PostConstruct;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import us.hogu.event.PaymentStatusChangedEvent;

@RequiredArgsConstructor
@Service
@Slf4j
@Transactional
public class StripeServiceImpl implements StripeService {

    @Value("${stripe.secret-key}")
    private String stripeSecretKey;

    @Value("${stripe.webhook-secret}")
    private String webhookSecret;

    @Value("${hogu.client.url}")
    private String clientUrl;

    private final PaymentJpa paymentJpa;
    private final ApplicationEventPublisher eventPublisher;

    // Costanti per gestione monetaria sicura
    private static final BigDecimal HUNDRED = new BigDecimal("100");
    private static final int MONEY_SCALE = 2;
    private static final RoundingMode MONEY_ROUNDING = RoundingMode.HALF_UP;

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

            // Conversione sicura da euro a centesimi
            Long stripeAmount = amount
                    .multiply(HUNDRED)
                    .setScale(0, MONEY_ROUNDING)
                    .longValueExact();

            String successUrl = request.getReturnUrl() != null ? request.getReturnUrl()
                    : clientUrl + "/payment/success";
            // Aggiungi parametro session_id per il recupero dati nel frontend
            if (!successUrl.contains("?")) {
                successUrl += "?paymentId={CHECKOUT_SESSION_ID}";
            } else {
                successUrl += "&paymentId={CHECKOUT_SESSION_ID}";
            }

            String cancelUrl = request.getCancelUrl() != null ? request.getCancelUrl() : clientUrl + "/payment/cancel";

            SessionCreateParams.Builder paramsBuilder = SessionCreateParams.builder()
                    .setMode(SessionCreateParams.Mode.PAYMENT)
                    .setSuccessUrl(successUrl)
                    .setCancelUrl(cancelUrl)
                    .setCustomerEmail(request.getCustomerEmail())
                    .putMetadata("booking_id", request.getBookingId() != null ? request.getBookingId().toString() : "")
                    .putMetadata("user_id", request.getUserId() != null ? request.getUserId().toString() : "")
                    .putMetadata("service_type", request.getServiceType().toString())
                    .setPaymentIntentData(
                            SessionCreateParams.PaymentIntentData.builder()
                                    .setCaptureMethod(SessionCreateParams.PaymentIntentData.CaptureMethod.MANUAL)
                                    .putMetadata("booking_id", request.getBookingId() != null ? request.getBookingId().toString() : "")
                                    .putMetadata("user_id", request.getUserId() != null ? request.getUserId().toString() : "")
                                    .putMetadata("service_type", request.getServiceType().toString())
                                    .build())
                    .addLineItem(
                            SessionCreateParams.LineItem.builder()
                                    .setQuantity(1L)
                                    .setPriceData(
                                            SessionCreateParams.LineItem.PriceData.builder()
                                                    .setCurrency(request.getCurrency().toLowerCase())
                                                    .setUnitAmount(stripeAmount)
                                                    .setProductData(
                                                            SessionCreateParams.LineItem.PriceData.ProductData.builder()
                                                                    .setName(request.getDescription() != null
                                                                            ? request.getDescription()
                                                                            : "Prenotazione Hogu")
                                                                    .build())
                                                    .build())
                                    .build());

            Session session = Session.create(paramsBuilder.build());

            return PaymentResponseDto.builder()
                    .paymentStatus(PaymentStatus.PENDING)
                    .paymentIdIntent(session.getId())
                    .approvalUrl(session.getUrl())
                    .amount(request.getAmount())
                    .currency(request.getCurrency())
                    .build();

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
            String resolvedPaymentIntentId = resolvePaymentIntentId(paymentIntentId);
            PaymentIntent paymentIntent = PaymentIntent.retrieve(resolvedPaymentIntentId);

            if ("succeeded".equals(paymentIntent.getStatus()) || "requires_capture".equals(paymentIntent.getStatus())) {
                return buildResponseFromIntent(paymentIntent);
            }

            Map<String, Object> params = new HashMap<>();
            params.put("return_url", "https://yourdomain.com/payment/success");
            paymentIntent = paymentIntent.confirm(params);

            if (!resolvedPaymentIntentId.equals(paymentIntentId)) {
                updatePaymentStatus(paymentIntentId, mapStripeStatusToPaymentStatus(paymentIntent.getStatus()));
            }

            return buildResponseFromIntent(paymentIntent);

        } catch (StripeException e) {
            throw new ValidationException(
                    ErrorConstants.ERROR_PAYMENT_STRIPE.name(),
                    ErrorConstants.ERROR_PAYMENT_STRIPE.getMessage());
        }
    }

    @Override
    public PaymentResponseDto capturePayment(String paymentIntentId) {
        try {
            String resolvedPaymentIntentId = resolvePaymentIntentId(paymentIntentId);
            PaymentIntent paymentIntent = PaymentIntent.retrieve(resolvedPaymentIntentId);

            if ("succeeded".equals(paymentIntent.getStatus())) {
                return buildResponseFromIntent(paymentIntent);
            }

            Map<String, Object> params = new HashMap<>();
            paymentIntent = paymentIntent.capture(params);

            PaymentStatus status = mapStripeStatusToPaymentStatus(paymentIntent.getStatus());
            if (!resolvedPaymentIntentId.equals(paymentIntentId)) {
                updatePaymentStatus(paymentIntentId, status);
            } else {
                updatePaymentStatus(resolvedPaymentIntentId, status);
            }

            return buildResponseFromIntent(paymentIntent);

        } catch (StripeException e) {
            throw new ValidationException(
                    ErrorConstants.ERROR_PAYMENT_STRIPE.name(),
                    "Impossibile catturare il pagamento: "
                            + (e.getStripeError() != null ? e.getStripeError().getMessage() : e.getMessage()));
        }
    }

    @Override
    public PaymentResponseDto voidPayment(String paymentIntentId) {
        try {
            String resolvedPaymentIntentId = resolvePaymentIntentId(paymentIntentId);
            PaymentIntent paymentIntent = PaymentIntent.retrieve(resolvedPaymentIntentId);

            PaymentIntent canceledIntent = paymentIntent.cancel();

            if (!resolvedPaymentIntentId.equals(paymentIntentId)) {
                updatePaymentStatus(paymentIntentId, PaymentStatus.VOIDED);
            } else {
                updatePaymentStatus(resolvedPaymentIntentId, PaymentStatus.VOIDED);
            }

            return buildResponseFromIntent(canceledIntent);

        } catch (StripeException e) {
            throw new ValidationException(
                    ErrorConstants.ERROR_PAYMENT_STRIPE.name(),
                    "Impossibile annullare il pagamento: "
                            + (e.getStripeError() != null ? e.getStripeError().getMessage() : e.getMessage()));
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
            params.put("capture_method", "manual"); // Deferred capture

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
            String resolvedPaymentIntentId = resolvePaymentIntentId(paymentIntentId);
            PaymentIntent paymentIntent = PaymentIntent.retrieve(resolvedPaymentIntentId);

            // Controllo: il pagamento deve essere completato per poter essere rimborsato
            if (!"succeeded".equals(paymentIntent.getStatus())) {
                throw new ValidationException(
                        ErrorConstants.ERROR_REFUND_STRIPE.name(),
                        "Il pagamento non è in stato completato");
            }

            Map<String, Object> params = new HashMap<>();
            params.put("payment_intent", resolvedPaymentIntentId);
            if (reason != null && !reason.trim().isEmpty()) {
                params.put("reason", reason.trim());
            }

            Refund refund = Refund.create(params);

            // Aggiorna lo stato del pagamento nel database
            if (!updatePaymentStatus(paymentIntentId, PaymentStatus.REFUNDED) && !resolvedPaymentIntentId.equals(paymentIntentId)) {
                updatePaymentStatus(resolvedPaymentIntentId, PaymentStatus.REFUNDED);
            }

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
            String resolvedPaymentIntentId = resolvePaymentIntentId(paymentIntentId);
            return PaymentIntent.retrieve(resolvedPaymentIntentId);
        } catch (StripeException e) {
            throw new ValidationException(
                    ErrorConstants.PAYMENT_NOT_FOUND.name(),
                    ErrorConstants.PAYMENT_NOT_FOUND.getMessage());
        }
    }

    @Override
    public boolean handleWebhook(String payload, String signature) {
        try {
            if (webhookSecret == null || webhookSecret.isBlank()) {
                throw new ValidationException(
                        ErrorConstants.PARAMS_NOT_VALID.name(),
                        "stripe.webhook-secret non configurato");
            }
            Event event = Webhook.constructEvent(payload, signature, webhookSecret);
            return processStripeEvent(event);
        } catch (SignatureVerificationException e) {
            throw new ValidationException(
                    ErrorConstants.PARAMS_NOT_VALID.name(),
                    "Firma webhook Stripe non valida");
        } catch (Exception e) {
            log.error("Errore durante l'elaborazione del webhook Stripe", e);
            throw e;
        }
    }

    @Override
    public boolean cancelPaymentIntent(String paymentIntentId) {
        try {
            String resolvedPaymentIntentId = resolvePaymentIntentId(paymentIntentId);
            PaymentIntent paymentIntent = PaymentIntent.retrieve(resolvedPaymentIntentId);
            PaymentIntent cancelledIntent = paymentIntent.cancel();

            if (!resolvedPaymentIntentId.equals(paymentIntentId)) {
                updatePaymentStatus(paymentIntentId, PaymentStatus.FAILED);
            } else {
                updatePaymentStatus(resolvedPaymentIntentId, PaymentStatus.FAILED);
            }

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
                    .amount(request.getAmount()) // torniamo l'importo originale in euro
                    .currency(request.getCurrency())
                    .build();
        }

        if ("requires_action".equals(status)) {
            String approvalUrl = null;
            if (paymentIntent.getNextAction() != null && paymentIntent.getNextAction().getRedirectToUrl() != null) {
                approvalUrl = paymentIntent.getNextAction().getRedirectToUrl().getUrl();
            }

            return PaymentResponseDto.builder()
                    .paymentStatus(PaymentStatus.PENDING)
                    .paymentIdIntent(paymentIntent.getId())
                    .clientSecret(paymentIntent.getClientSecret())
                    .approvalUrl(approvalUrl)
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

        String approvalUrl = null;
        if (paymentIntent.getNextAction() != null && paymentIntent.getNextAction().getRedirectToUrl() != null) {
            approvalUrl = paymentIntent.getNextAction().getRedirectToUrl().getUrl();
        }

        return PaymentResponseDto.builder()
                .paymentStatus(status)
                .paymentIdIntent(paymentIntent.getId())
                .clientSecret(paymentIntent.getClientSecret())
                .approvalUrl(approvalUrl)
                .amount(amountInEuro)
                .currency(paymentIntent.getCurrency())
                .build();
    }

    private PaymentStatus mapStripeStatusToPaymentStatus(String stripeStatus) {
        if ("succeeded".equals(stripeStatus)) {
            return PaymentStatus.COMPLETED;
        }
        if ("requires_capture".equals(stripeStatus)) {
            return PaymentStatus.AUTHORIZED;
        }
        if ("requires_action".equals(stripeStatus) || "requires_payment_method".equals(stripeStatus)) {
            return PaymentStatus.PENDING;
        }
        if ("canceled".equals(stripeStatus)) {
            return PaymentStatus.VOIDED;
        }
        // payment_failed, ecc...
        return PaymentStatus.FAILED;
    }

    private String resolvePaymentIntentId(String paymentId) throws StripeException {
        if (paymentId == null || paymentId.isBlank()) {
            throw new ValidationException(
                    ErrorConstants.PARAMS_NOT_VALID.name(),
                    ErrorConstants.PARAMS_NOT_VALID.getMessage());
        }

        if (paymentId.startsWith("cs_")) {
            Session session = Session.retrieve(paymentId);
            String paymentIntentId = session.getPaymentIntent();
            if (paymentIntentId == null || paymentIntentId.isBlank()) {
                throw new ValidationException(
                        ErrorConstants.PAYMENT_NOT_FOUND.name(),
                        ErrorConstants.PAYMENT_NOT_FOUND.getMessage());
            }
            return paymentIntentId;
        }

        return paymentId;
    }

    private boolean processStripeEvent(Event event) {
        String eventType = event.getType();

        if ("checkout.session.completed".equals(eventType)
                || "checkout.session.async_payment_succeeded".equals(eventType)) {
            return handleCheckoutSessionCompleted(event);
        }
        if ("payment_intent.amount_capturable_updated".equals(eventType)) {
            return handlePaymentAuthorized(event);
        }
        if ("payment_intent.succeeded".equals(eventType)) {
            return handlePaymentSuccess(event);
        }
        if ("payment_intent.payment_failed".equals(eventType)) {
            return handlePaymentFailure(event);
        }
        if ("payment_intent.canceled".equals(eventType)) {
            return handlePaymentCancelled(event);
        }

        return true;
    }

    private boolean handleCheckoutSessionCompleted(Event event) {
        Session session = deserializeEventObject(event, Session.class);

        // Se c'è un payment intent associato, recuperalo per capire lo stato effettivo
        if (session.getPaymentIntent() != null) {
            try {
                PaymentIntent pi = PaymentIntent.retrieve(session.getPaymentIntent());
                if ("requires_capture".equals(pi.getStatus())) {
                    updatePaymentStatus(session.getId(), PaymentStatus.AUTHORIZED);
                    return true;
                }
            } catch (StripeException e) {
                log.error("Errore nel recupero PaymentIntent {} da CheckoutSession {}", session.getPaymentIntent(), session.getId(), e);
            }
        }

        // Default (se capture_method fosse cambiato a automatic)
        updatePaymentStatus(session.getId(), PaymentStatus.COMPLETED);
        return true;
    }

    private boolean handlePaymentAuthorized(Event event) {
        PaymentIntent paymentIntent = deserializeEventObject(event, PaymentIntent.class);
        if ("requires_capture".equals(paymentIntent.getStatus())) {
            boolean updated = updatePaymentStatus(paymentIntent.getId(), PaymentStatus.AUTHORIZED);
            if (!updated) {
                updateLatestStripePaymentByBookingId(paymentIntent, PaymentStatus.AUTHORIZED);
            }
        }
        return true;
    }

    private boolean handlePaymentSuccess(Event event) {
        PaymentIntent paymentIntent = deserializeEventObject(event, PaymentIntent.class);
        boolean updated = updatePaymentStatus(paymentIntent.getId(), PaymentStatus.COMPLETED);
        if (!updated) {
            updateLatestStripePaymentByBookingId(paymentIntent, PaymentStatus.COMPLETED);
        }
        return true;
    }

    private boolean handlePaymentFailure(Event event) {
        PaymentIntent paymentIntent = deserializeEventObject(event, PaymentIntent.class);
        boolean updated = updatePaymentStatus(paymentIntent.getId(), PaymentStatus.FAILED);
        if (!updated) {
            updateLatestStripePaymentByBookingId(paymentIntent, PaymentStatus.FAILED);
        }
        return true;
    }

    private boolean handlePaymentCancelled(Event event) {
        PaymentIntent paymentIntent = deserializeEventObject(event, PaymentIntent.class);
        boolean updated = updatePaymentStatus(paymentIntent.getId(), PaymentStatus.FAILED);
        if (!updated) {
            updateLatestStripePaymentByBookingId(paymentIntent, PaymentStatus.FAILED);
        }
        return true;
    }

    private <T extends StripeObject> T deserializeEventObject(Event event, Class<T> expectedClass) {
        EventDataObjectDeserializer deserializer = event.getDataObjectDeserializer();
        StripeObject stripeObject = deserializer.getObject().orElse(null);
        if (expectedClass.isInstance(stripeObject)) {
            return expectedClass.cast(stripeObject);
        }

        String rawJson = null;
        try {
            rawJson = deserializer.getRawJson();
        } catch (Exception ignored) {
        }

        if (rawJson != null && !rawJson.isBlank()) {
            try {
                return new Gson().fromJson(rawJson, expectedClass);
            } catch (Exception ignored) {
            }
        }

        try {
            StripeObject unsafeObject = deserializer.deserializeUnsafe();
            if (expectedClass.isInstance(unsafeObject)) {
                return expectedClass.cast(unsafeObject);
            }
        } catch (Exception ignored) {
        }

        throw new IllegalStateException("Impossibile deserializzare l'oggetto Stripe per evento " + event.getType());
    }

    private boolean updatePaymentStatus(String paymentIdIntent, PaymentStatus status) {
        return paymentJpa.findByPaymentIdIntent(paymentIdIntent)
                .map(payment -> {
                    payment.setStatus(status);
                    payment.setLastUpdateDate(OffsetDateTime.now());
                    Payment savedPayment = paymentJpa.save(payment);
                    eventPublisher.publishEvent(new PaymentStatusChangedEvent(this, savedPayment));
                    return true;
                })
                .orElse(false);
    }

    private boolean updateLatestStripePaymentByBookingId(PaymentIntent paymentIntent, PaymentStatus status) {
        Map<String, String> metadata = paymentIntent.getMetadata();
        if (metadata == null) {
            return false;
        }

        String bookingIdRaw = metadata.get("booking_id");
        if (bookingIdRaw == null || bookingIdRaw.isBlank()) {
            return false;
        }

        Long bookingId;
        try {
            bookingId = Long.parseLong(bookingIdRaw);
        } catch (NumberFormatException e) {
            return false;
        }

        List<Payment> payments = paymentJpa.findByBooking_Id(bookingId);
        Payment latestStripePayment = payments.stream()
                .filter(p -> p.getPaymentMethod() == PaymentMethod.STRIPE)
                .max((p1, p2) -> p1.getId().compareTo(p2.getId()))
                .orElse(null);

        if (latestStripePayment == null) {
            return false;
        }

        latestStripePayment.setStatus(status);
        latestStripePayment.setLastUpdateDate(OffsetDateTime.now());
        Payment savedPayment = paymentJpa.save(latestStripePayment);
        eventPublisher.publishEvent(new PaymentStatusChangedEvent(this, savedPayment));
        return true;
    }

    @Override
    public boolean processRefund(String paymentIntentId, String reason) {
        // TODO Auto-generated method stub
        return false;
    }
}
