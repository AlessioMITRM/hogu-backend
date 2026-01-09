package us.hogu.repository.projection;

public interface ClubDetailProjection {
	Long getId();

	String getName();

	String getDescription();

	String getAddress();

	Integer getCapacity();

	String getEvents(); // JSON degli eventi

	Double getBasePrice();

	String getImages();

	Long getProviderId();

	String getProviderName();
}
