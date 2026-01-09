package us.hogu.service.intefaces;

import java.util.List;

import us.hogu.client.feign.dto.request.PayPalPaymentRequestDto;
import us.hogu.client.feign.dto.request.StripePaymentRequestDto;
import us.hogu.client.feign.dto.response.PaymentResponseDto;
import us.hogu.model.Payment;

public interface PaymentService {

	PaymentResponseDto processStripePayment(StripePaymentRequestDto requestDto, Long userId);

	PaymentResponseDto processPayPalPayment(PayPalPaymentRequestDto requestDto, Long userId);

	PaymentResponseDto executePayPalPayment(String paymentId, String payerId, Long userId);

	List<Payment> getUserPayments(Long userId);

	PaymentResponseDto getPaymentByIdBooking(Long bookingId, Long userId);

	void requestRefund(Long paymentId, Long userId, String reason);

}
