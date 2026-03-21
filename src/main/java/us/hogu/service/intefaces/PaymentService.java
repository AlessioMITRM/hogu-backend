package us.hogu.service.intefaces;

import java.util.List;

import us.hogu.client.feign.dto.request.PayPalPaymentRequestDto;
import us.hogu.client.feign.dto.request.StripePaymentRequestDto;
import us.hogu.client.feign.dto.response.PaymentResponseDto;
import us.hogu.model.Payment;

import us.hogu.model.enums.ServiceType;

public interface PaymentService {

	PaymentResponseDto processStripePayment(StripePaymentRequestDto requestDto, Long userId);

	PaymentResponseDto processPayPalPayment(PayPalPaymentRequestDto requestDto, Long userId);

	PaymentResponseDto executePayPalPayment(String paymentId, String payerId, Long userId);

	List<Payment> getUserPayments(Long userId);

	PaymentResponseDto getPaymentByIdBooking(Long bookingId, Long userId);

	us.hogu.controller.dto.response.BookingInfoDTO getBookingInfoByPaymentId(String paymentId, Long userId);
	
	us.hogu.controller.dto.response.BookingInfoDTO getPendingBooking(Long userId);
    
    void cancelBooking(Long bookingId, ServiceType serviceType, Long userId);
    
    void cancelBookingByProvider(Long bookingId, ServiceType serviceType, Long providerId, String reason);
    
    void confirmBookingByProvider(Long bookingId, ServiceType serviceType, Long providerId);

    void completeBookingByProvider(Long bookingId, ServiceType serviceType, Long providerId);

	void requestRefund(Long paymentId, Long userId, String reason);
}
