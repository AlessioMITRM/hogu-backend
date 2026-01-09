package us.hogu.repository.projection;

import java.time.OffsetDateTime;

import us.hogu.model.enums.BookingStatus;

public interface NccBookingProjection {
	Long getId();
    OffsetDateTime getPickupTime();
    String getPickupLocation();
    String getDestination();
    BookingStatus getStatus();
    Double getTotalAmount();
    OffsetDateTime getCreationDate();
    String getCustomerName();
    String getCustomerEmail();
    String getNccServiceName();
}
