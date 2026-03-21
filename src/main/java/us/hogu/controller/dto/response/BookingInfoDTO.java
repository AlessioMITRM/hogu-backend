package us.hogu.controller.dto.response;

import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import us.hogu.model.enums.BookingStatus;
import us.hogu.model.enums.PaymentStatus;
import us.hogu.model.enums.ServiceType;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BookingInfoDTO {
    private String bookingId;
    private String serviceName;
    private String bookingDate; // Formattata come stringa o LocalDate
    private BigDecimal amount;
    private String providerName;
    private BookingStatus bookingStatus;
    private PaymentStatus paymentStatus;
    private us.hogu.model.enums.PaymentMethod paymentMethod;
    private ServiceType serviceType;
    private String statusReason;
}
