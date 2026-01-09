package us.hogu.service.impl;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.paypal.api.payments.*;
import com.paypal.base.rest.APIContext;
import com.paypal.base.rest.PayPalRESTException;

import us.hogu.model.Payment;
import us.hogu.model.enums.PaymentStatus;
import us.hogu.repository.jpa.PaymentJpa;
import us.hogu.service.intefaces.PayPalService;
import us.hogu.client.feign.dto.request.PayPalPaymentRequestDto;
import us.hogu.client.feign.dto.response.PaymentResponseDto;
import us.hogu.common.constants.ErrorConstants;
import us.hogu.exception.ValidationException;

import javax.annotation.PostConstruct;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@Transactional
public class PayPalServiceImpl implements PayPalService {

    @Value("${paypal.client-id}")
    private String clientId;

    @Value("${paypal.client-secret}")
    private String clientSecret;

    @Value("${paypal.mode}")
    private String mode; // "sandbox" or "live"

    private APIContext apiContext;
    private final PaymentJpa paymentJpa;

    
    public PayPalServiceImpl(PaymentJpa paymentJpa) {
        this.paymentJpa = paymentJpa;
    }

    
    @PostConstruct
    public void init() {
        this.apiContext = new APIContext(clientId, clientSecret, mode);
    }

    
    @Override
    public PaymentResponseDto createPayment(PayPalPaymentRequestDto request) {
        try {
            // Amount
            Amount amount = new Amount();
            amount.setCurrency(request.getCurrency());
            amount.setTotal(String.format("%.2f", request.getAmount()));

            // Transaction
            Transaction transaction = new Transaction();
            transaction.setAmount(amount);
            transaction.setDescription(request.getDescription());
            
            // Item list se fornita
            if (request.getItems() != null && !request.getItems().isEmpty()) {
                ItemList itemList = new ItemList();
                List<Item> items = new ArrayList<>();
                
                for (PayPalPaymentRequestDto.PayPalItem itemDto : request.getItems()) {
                    Item item = new Item();
                    item.setName(itemDto.getName())
                        .setDescription(itemDto.getDescription())
                        .setCurrency(request.getCurrency())
                        .setPrice(String.format("%.2f", itemDto.getPrice()))
                        .setQuantity(String.valueOf(itemDto.getQuantity()))
                        .setSku(itemDto.getSku());
                    items.add(item);
                }
                itemList.setItems(items);
                transaction.setItemList(itemList);
            }

            List<Transaction> transactions = new ArrayList<>();
            transactions.add(transaction);

            // Payer
            Payer payer = new Payer();
            payer.setPaymentMethod("paypal");

            // Payment
            com.paypal.api.payments.Payment payment = new com.paypal.api.payments.Payment();
            payment.setIntent("sale");
            payment.setPayer(payer);
            payment.setTransactions(transactions);

            // Redirect URLs
            RedirectUrls redirectUrls = new RedirectUrls();
            redirectUrls.setCancelUrl("https://yourdomain.com/payment/paypal/cancel");
            redirectUrls.setReturnUrl("https://yourdomain.com/payment/paypal/success");
            payment.setRedirectUrls(redirectUrls);

            // Create payment
            com.paypal.api.payments.Payment createdPayment = payment.create(apiContext);

            // CORREZIONE: usa Links invece di LinkDescription
            String approvalUrl = createdPayment.getLinks().stream()
                    .filter(link -> link.getRel().equals("approval_url"))
                    .findFirst()
                    .map(Links::getHref) // CORREZIONE: Links invece di LinkDescription
                    .orElse(null);

            return PaymentResponseDto.builder()
                .paymentStatus(PaymentStatus.PENDING)
                .paymentIdIntent(createdPayment.getId())
                .amount(request.getAmount())
                .currency(request.getCurrency())
                .build();

        } catch (PayPalRESTException e) {
            throw new ValidationException(ErrorConstants.ERROR_PAYMENT_PAYPAL.name(), ErrorConstants.ERROR_PAYMENT_PAYPAL.getMessage());

        } catch (Exception e) {
            throw new ValidationException(ErrorConstants.ERROR_PAYMENT_PAYPAL.name(), ErrorConstants.ERROR_PAYMENT_PAYPAL.getMessage());

        }
    }
    
    @Override
    public PaymentResponseDto executePayment(String paymentId, String payerId) {
        try {
            // Retrieve the payment
            com.paypal.api.payments.Payment payment = new com.paypal.api.payments.Payment();
            payment.setId(paymentId);

            // Payment execution
            PaymentExecution paymentExecution = new PaymentExecution();
            paymentExecution.setPayerId(payerId);

            // Execute payment
            com.paypal.api.payments.Payment executedPayment = payment.execute(apiContext, paymentExecution);

            // Check payment state
            if ("approved".equals(executedPayment.getState())) {
                updatePaymentStatus(paymentId, PaymentStatus.COMPLETED);
                return PaymentResponseDto.builder()
                    .paymentStatus(PaymentStatus.COMPLETED)
                    .paymentIdIntent(executedPayment.getId())
                    .amount(Double.parseDouble(executedPayment.getTransactions().get(0).getAmount().getTotal()))
                    .currency(executedPayment.getTransactions().get(0).getAmount().getCurrency())
                    .build();
            } else {
                updatePaymentStatus(paymentId, PaymentStatus.FAILED);
                return PaymentResponseDto.builder()
                    .paymentStatus(PaymentStatus.FAILED)
                    .paymentIdIntent(executedPayment.getId())
                    .errorMessage("Stato pagamento: " + executedPayment.getState())
                    .build();
            }

        } catch (PayPalRESTException e) {
            updatePaymentStatus(paymentId, PaymentStatus.FAILED);
            
            throw new ValidationException(ErrorConstants.ERROR_PAYMENT_PAYPAL.name(), ErrorConstants.ERROR_PAYMENT_PAYPAL.getMessage());
        }
    }

    @Override
    public PaymentResponseDto getPaymentDetails(String paymentId) {
        try {
            com.paypal.api.payments.Payment payment = com.paypal.api.payments.Payment.get(apiContext, paymentId);
            
            PaymentStatus status = mapPayPalStateToPaymentStatus(payment.getState());
            
            return PaymentResponseDto.builder()
                .paymentStatus(status)
                .paymentIdIntent(payment.getId())
                .amount(Double.parseDouble(payment.getTransactions().get(0).getAmount().getTotal()))
                .currency(payment.getTransactions().get(0).getAmount().getCurrency())
                .build();

        } catch (PayPalRESTException e) {
            throw new ValidationException(ErrorConstants.ERROR_PAYMENT_PAYPAL.name(), ErrorConstants.ERROR_PAYMENT_PAYPAL.getMessage());
        }
    }

    @Override
    public boolean refundPayment(String paymentId, String reason) {
        try {
            // Prima recupera il sale ID dal pagamento
            com.paypal.api.payments.Payment payment = com.paypal.api.payments.Payment.get(apiContext, paymentId);
            String saleId = payment.getTransactions().get(0).getRelatedResources().get(0).getSale().getId();
            
            // Crea il refund
            RefundRequest refundRequest = new RefundRequest();
            
            // Amount per rimborso parziale (opzionale)
            // Amount amount = new Amount();
            // amount.setCurrency("EUR");
            // amount.setTotal("10.00");
            // refundRequest.setAmount(amount);
            
            refundRequest.setReason(reason);
            
            // Esegui il refund
            Sale sale = new Sale();
            sale.setId(saleId);
            Refund refund = sale.refund(apiContext, refundRequest);
            
            if ("completed".equals(refund.getState())) {
                updatePaymentStatus(paymentId, PaymentStatus.FAILED);
                return true;
            }
            
            return false;

        } catch (PayPalRESTException e) {
            throw new ValidationException(ErrorConstants.ERROR_REFUND_PAYPAL.name(), ErrorConstants.ERROR_REFUND_PAYPAL.getMessage());

        }
    }

    @Override
    public boolean cancelPayment(String paymentId) {
        try {
            // PayPal non permette direttamente la cancellazione di un pagamento
            // Possiamo solo aggiornare lo stato nel nostro database
            updatePaymentStatus(paymentId, PaymentStatus.FAILED);
            return true;

        } catch (Exception e) {
            throw new ValidationException(ErrorConstants.ERROR_PAYMENT_DELETE_PAYPAL.name(), ErrorConstants.ERROR_PAYMENT_DELETE_PAYPAL.getMessage());
        }
    }

    // METODI PRIVATI DI SUPPORTO
    private void updatePaymentStatus(String paymentId, PaymentStatus status) {
        paymentJpa.findByPaymentIdIntent(paymentId)
            .ifPresent(payment -> {
                payment.setStatus(status);
                payment.setLastUpdateDate(OffsetDateTime.now());
                paymentJpa.save(payment);
            });
    }

    private PaymentStatus mapPayPalStateToPaymentStatus(String paypalState) {
        switch (paypalState.toLowerCase()) {
            case "created":
            case "pending":
                return PaymentStatus.PENDING;
            case "approved":
            case "completed":
                return PaymentStatus.COMPLETED;
            case "failed":
            case "canceled":
            case "expired":
            default:
                return PaymentStatus.FAILED;
        }
    }
}