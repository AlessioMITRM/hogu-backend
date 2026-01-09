package us.hogu.repository.projection;

import java.time.OffsetDateTime;

public interface ReviewSummaryProjection {
	Long getId();

	Integer getRating();

	String getComment();

	OffsetDateTime getDataCreazione();

	String getUserName();
}
