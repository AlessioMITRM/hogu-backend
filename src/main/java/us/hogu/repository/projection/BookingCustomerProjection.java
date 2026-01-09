package us.hogu.repository.projection;

import java.time.OffsetDateTime;

import us.hogu.model.enums.BookingStatus;
import us.hogu.model.enums.ServiceType;

public interface BookingCustomerProjection {
    Long getId();
    
    BookingStatus getStatus();
    
    OffsetDateTime getReservationTime();
    
    String getServiceName();
    
    ServiceType getServiceType();
    
    Double getAmountPaid();
}