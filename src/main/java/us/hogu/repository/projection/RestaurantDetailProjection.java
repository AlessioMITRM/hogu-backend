package us.hogu.repository.projection;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

import us.hogu.model.ServiceLocale;

public interface RestaurantDetailProjection {
	Long getId();

	String getName();

	String getDescription();

	List<ServiceLocale> getLocales();
	
	String getMenu(); // JSON del menu

	Integer getCapacity();

	Double getBasePrice();

	String getImages();

	Long getProviderId();

	String getProviderName();

	OffsetDateTime getCreationDate();
}
