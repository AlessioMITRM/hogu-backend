package us.hogu.controller.dto.response;

import java.time.OffsetDateTime;

import us.hogu.model.enums.BookingStatus;
import us.hogu.model.enums.ServiceType;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class NccBookingResponseDto {
    private Long id;
    private ServiceType serviceType;
    private Long serviceId;
    private String serviceName;
    private BookingStatus status;
    private Double totalAmount;
    private OffsetDateTime creationDate;
    
    // Campi specifici NCC
    private OffsetDateTime pickupTime;
    private String pickupLocation;
    private String destination;
}