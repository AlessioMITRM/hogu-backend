package us.hogu.service.intefaces;

import com.stripe.model.PaymentIntent;

import us.hogu.client.feign.dto.request.StripePaymentRequestDto;
import us.hogu.client.feign.dto.response.PaymentResponseDto;

public interface StripeService {

	PaymentResponseDto processPayment(StripePaymentRequestDto request);

	PaymentResponseDto confirmPayment(String paymentIntentId);

	PaymentIntent createPaymentIntent(Double amount, String currency, String bookingId);

	boolean processRefund(String paymentIntentId, String reason);

	PaymentIntent getPaymentIntent(String paymentIntentId);

	boolean handleWebhook(String payload, String signature);

	boolean cancelPaymentIntent(String paymentIntentId);

	boolean refundPayment(String paymentIntentId, String reason);

}
