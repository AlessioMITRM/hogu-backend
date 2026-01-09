package us.hogu.repository.projection;

import java.time.OffsetDateTime;

import us.hogu.model.enums.BookingStatus;
import us.hogu.model.enums.ServiceType;

public interface BookingSummaryProjection {
	Long getId();

	BookingStatus getStatus();

	OffsetDateTime getCreationDate();

	OffsetDateTime getReservationTime();

	Double getTotalAmount();

	String getServiceName();

	ServiceType getServiceType();
}
