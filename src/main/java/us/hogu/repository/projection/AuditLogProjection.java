package us.hogu.repository.projection;

import java.time.OffsetDateTime;

public interface AuditLogProjection {
	Long getId();

	String getAction();

	String getEntity();

	Long getEntityId();

	OffsetDateTime getCreationDate();

	String getAdminName();
}
