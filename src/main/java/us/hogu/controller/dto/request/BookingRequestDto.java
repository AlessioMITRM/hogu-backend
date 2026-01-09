package us.hogu.controller.dto.request;

import java.time.OffsetDateTime;

import lombok.Data;
import us.hogu.model.enums.ServiceType;

@Data
public class BookingRequestDto {
    private ServiceType serviceType; // RESTAURANT, NCC, CLUB, LUGGAGE
    
    private Long serviceId;
    
    private OffsetDateTime reservationTime;
    
    private Integer numberOfPeople;
}
