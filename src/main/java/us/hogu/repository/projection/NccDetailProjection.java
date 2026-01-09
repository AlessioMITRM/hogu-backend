package us.hogu.repository.projection;

import java.util.List;

import us.hogu.model.ServiceLocale;

public interface NccDetailProjection {
	Long getId();

	String getName();

	String getDescription();

	String getVehiclesAvailable();

	Double getBasePrice();

	List<ServiceLocale> getLocales();

	String getImages();

	Long getProviderId();

	String getProviderName();
	
	String getSeatsVehicle();
}
