package us.hogu.repository.projection;

import java.time.OffsetDateTime;

public interface SupportTicketProjection {
	Long getId();

	String getSubject();

	String getStatus();

	OffsetDateTime getCreationDate();

	String getUserName();
}
