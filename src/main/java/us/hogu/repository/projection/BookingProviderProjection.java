package us.hogu.repository.projection;

import java.time.OffsetDateTime;

import us.hogu.model.enums.BookingStatus;

public interface BookingProviderProjection {
	Long getId();

	BookingStatus getStatus();

	OffsetDateTime getReservationTime();

	String getCustomerName();

	String getCustomerEmail();

	Integer getNumberOfPeople();

	String getSpecialRequests();
}
