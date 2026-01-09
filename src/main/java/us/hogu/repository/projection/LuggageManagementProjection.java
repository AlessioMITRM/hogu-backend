package us.hogu.repository.projection;

import java.time.OffsetDateTime;

public interface LuggageManagementProjection {
	Long getId();

	String getName();

	Boolean getPublicationStatus();

	OffsetDateTime getCreationDate();

	Integer getCapacity();

	String getProviderName();
}
