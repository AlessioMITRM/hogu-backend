package us.hogu.repository.jpa;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import us.hogu.controller.dto.response.ClubInfoStatsDto;
import us.hogu.controller.dto.response.InfoStatsDto;
import us.hogu.model.LuggageServiceEntity;
import us.hogu.model.User;

public interface LuggageServiceJpa extends JpaRepository<LuggageServiceEntity, Long> {
	
	Optional<LuggageServiceEntity> findByIdAndUserId(Long serviceId, Long providerId);
	
	@Query("SELECT l FROM LuggageServiceEntity l WHERE l.publicationStatus = true")
	Page<LuggageServiceEntity> findActiveSummaries(Pageable pageable);

	@Query("SELECT DISTINCT l FROM LuggageServiceEntity l " +
		"LEFT JOIN l.locales loc " +
		"WHERE l.publicationStatus = true " +
		"AND (:searchText IS NULL OR " +
		"     LOWER(l.name) LIKE LOWER(CONCAT('%', :searchText, '%')) OR " +
		"     EXISTS ( " +
		"         SELECT 1 FROM ServiceLocale sl " +
		"         WHERE sl MEMBER OF l.locales " +
		"           AND LOWER(sl.city) LIKE LOWER(CONCAT('%', :searchText, '%')) " +
		"     )" +
		")")
	Page<LuggageServiceEntity> findActiveBySearch(@Param("searchText") String searchText, Pageable pageable);

	@Query("SELECT l FROM LuggageServiceEntity l WHERE l.user.id = :providerId")
	Optional<LuggageServiceEntity> findByProviderIdForSingleService(Long providerId);
	
	@Query("SELECT l FROM LuggageServiceEntity l WHERE l.user.id = :providerId")
	Page<LuggageServiceEntity> findByProviderId(Long providerId, Pageable pageable);

	@Query("SELECT l FROM LuggageServiceEntity l WHERE l.id = :id AND l.user.id = :providerId")
	Optional<LuggageServiceEntity> findDetailByIdAndProvider(Long id, Long providerId);

	List<LuggageServiceEntity> findAllByUserId(Long providerId);
	
	@Query("SELECT l FROM LuggageServiceEntity l")
	List<LuggageServiceEntity> findAllForAdmin();

	@Query("SELECT l FROM LuggageServiceEntity l WHERE l.publicationStatus = false")
	List<LuggageServiceEntity> findPendingApproval();

	boolean existsByIdAndUserId(Long id, Long providerId);

	boolean existsByIdAndPublicationStatusTrue(Long id);

	@Query("SELECT COUNT(l) FROM LuggageServiceEntity l WHERE l.user.id = :providerId AND l.publicationStatus = true")
	Long countActiveByProvider(Long providerId);

	@Query("SELECT COUNT(l) FROM LuggageServiceEntity l WHERE l.user.id = :providerId")
	Long countTotalByProvider(Long providerId);

	@Query("SELECT l FROM LuggageServiceEntity l " +
			"LEFT JOIN FETCH l.locales loc " +
			"WHERE l.id = :id AND l.publicationStatus = true " +
			"AND (:language IS NULL OR LOWER(loc.language) = LOWER(:language))")
	Optional<LuggageServiceEntity> findDetailById(@Param("id") Long id, @Param("language") String language);

	@Query("SELECT new us.hogu.controller.dto.response.InfoStatsDto(" +
            "   CAST(NULL as java.lang.Long), " +
            "   (SELECT COUNT(b) FROM LuggageBooking b WHERE b.luggageService.user.id = :providerId), " +
            "   (SELECT COALESCE(SUM(b.totalAmount), 0) FROM LuggageBooking b WHERE b.luggageService.user.id = :providerId) " +
            ") " +
            "FROM User u WHERE u.id = :providerId")
	InfoStatsDto getInfoStatsByProviderId(@Param("providerId") Long providerId);

	List<LuggageServiceEntity> findByUser(User user);
}
