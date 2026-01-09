package us.hogu.converter;

import java.time.OffsetDateTime;

import org.springframework.stereotype.Component;

import us.hogu.controller.dto.request.PaymentRequestDto;
import us.hogu.controller.dto.response.PaymentResponseDto;
import us.hogu.controller.dto.response.PayoutResponseDto;
import us.hogu.model.Booking;
import us.hogu.model.Payment;
import us.hogu.model.Payout;
import us.hogu.model.User;
import us.hogu.model.enums.PaymentStatus;

@Component
public class PaymentMapper {
    
    public Payment toEntity(PaymentRequestDto dto, Booking booking, User user) {
        return Payment.builder()
            .booking(booking)
            .user(user)
            .amount(dto.getAmount())
            .currency(dto.getCurrency())
            .paymentMethod(dto.getPaymentMethod())
            .status(PaymentStatus.PENDING)
            .build();
    }
    
    public PaymentResponseDto toResponseDto(Payment payment) {
        return PaymentResponseDto.builder()
            .id(payment.getId())
            .bookingId(payment.getBooking().getId())
            .amount(payment.getAmount())
            .currency(payment.getCurrency())
            .paymentMethod(payment.getPaymentMethod())
            .status(payment.getStatus())
            .feeAmount(payment.getFeeAmount())
            .netAmount(payment.getNetAmount())
            .build();
    }
    
    
    public PayoutResponseDto toPayoutResponseDto(Payout payout) {
        return PayoutResponseDto.builder()
            .id(payout.getId())
            .providerId(payout.getProvider().getId())
            .amount(payout.getAmount())
            .status(payout.getStatus())
            .paymentDate(payout.getPaymentDate())
            .build();
    }
}
