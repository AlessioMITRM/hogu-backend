package us.hogu.controller.dto.response;

import java.time.OffsetDateTime;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class BookingDetailResponseDto {
    private Object serviceDetails;
    
    private Object bookingDetails;
    
    private PaymentResponseDto payment;
}
