package us.hogu.service.intefaces;

import us.hogu.client.feign.dto.request.PayPalPaymentRequestDto;
import us.hogu.client.feign.dto.response.PaymentResponseDto;

public interface PayPalService {

	PaymentResponseDto createPayment(PayPalPaymentRequestDto request);

	PaymentResponseDto executePayment(String paymentId, String payerId);

	PaymentResponseDto getPaymentDetails(String paymentId);

	boolean refundPayment(String paymentId, String reason);

	boolean cancelPayment(String paymentId);

}
