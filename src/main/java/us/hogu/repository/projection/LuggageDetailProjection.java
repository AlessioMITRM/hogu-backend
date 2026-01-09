package us.hogu.repository.projection;

import java.util.List;

import us.hogu.model.ServiceLocale;

public interface LuggageDetailProjection {
	Long getId();

	String getName();

	String getDescription();

	List<ServiceLocale> getLocales();

	Integer getCapacity();

	Double getBasePrice();

	List<String> getImages();

	Long getProviderId();

	String getProviderName();
}
