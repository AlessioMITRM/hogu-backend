package us.hogu.repository.projection;

import java.time.OffsetDateTime;

import us.hogu.model.enums.PaymentStatus;

public interface PaymentSummaryProjection {
	Long getId();

	Double getAmount();

	PaymentStatus getStatus();

	OffsetDateTime getCreationDate();

	String getPaymentMethod();
}
