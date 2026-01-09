package us.hogu.repository.projection;

public interface AdminDashboardProjection {
	Long getTotalUsers();

	Long getTotalProviders();

	Long getTotalBookingsToday();

	Double getRevenueToday();
}
