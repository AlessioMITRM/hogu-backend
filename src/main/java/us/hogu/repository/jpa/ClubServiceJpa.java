package us.hogu.repository.jpa;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import us.hogu.controller.dto.response.ClubInfoStatsDto;
import us.hogu.model.ClubServiceEntity;
import us.hogu.model.EventClubServiceEntity;
import us.hogu.model.User;
import us.hogu.repository.projection.ClubDetailProjection;
import us.hogu.repository.projection.ClubManagementProjection;
import us.hogu.repository.projection.ClubSummaryProjection;

public interface ClubServiceJpa extends JpaRepository<ClubServiceEntity, Long> {

	boolean existsByIdAndUserId(Long id, Long userId);

	Optional<ClubServiceEntity> findById(Long id);

	@Query("SELECT e FROM EventClubServiceEntity e WHERE e.clubService.id = :clubId AND e.deleted = false")
	Page<EventClubServiceEntity> findByClubServiceId(@Param("clubId") Long clubId, Pageable pageable);

	@Query("SELECT e FROM EventClubServiceEntity e WHERE e.clubService.id = :clubId AND e.deleted = false AND e.isActive = true")
	Page<EventClubServiceEntity> findActiveEventsByClubServiceId(@Param("clubId") Long clubId, Pageable pageable);

	@Query("SELECT e FROM EventClubServiceEntity e " +
			"JOIN e.clubService cs " +
			"WHERE cs.id = :clubId AND e.id = :eventId AND e.deleted = false")
	Optional<EventClubServiceEntity> findEventByClubIdAndEventId(
			@Param("clubId") Long clubId,
			@Param("eventId") Long eventId);

	@Query("SELECT e " +
			"FROM EventClubServiceEntity e " +
			"WHERE e.clubService.id = :clubId " +
			"AND e.startTime >= :startOfDay " +
			"AND e.startTime < :endOfDay " +
			"AND e.deleted = false")
	Page<EventClubServiceEntity> findTodayEventsByClubServiceId(
			@Param("clubId") Long clubId,
			@Param("startOfDay") OffsetDateTime startOfDay,
			@Param("endOfDay") OffsetDateTime endOfDay,
			Pageable pageable);

	@Query("SELECT c.id as id, c.name as name, c.description as description, "
			+ "c.basePrice as basePrice, c.locales as locales, c.images as images "
			+ "FROM ClubServiceEntity c WHERE c.publicationStatus = true")
	List<ClubServiceEntity> findActiveSummaries();

	@Query("SELECT DISTINCT c FROM ClubServiceEntity c " +
			"LEFT JOIN c.locales l " +
			"WHERE c.publicationStatus = true " +
			"AND (:searchText IS NULL OR " +
			"     LOWER(c.name) LIKE LOWER(CONCAT('%', :searchText, '%')) OR " +
			"     EXISTS ( " +
			"         SELECT 1 FROM ServiceLocale sl " +
			"         WHERE sl MEMBER OF c.locales " +
			"           AND LOWER(sl.province) LIKE LOWER(CONCAT('%', :searchText, '%')) " +
			"     )" +
			") " +
			"AND (COALESCE(:language, '') = '' " +
			"     OR LOWER(l.language) IN (LOWER(:language), 'en'))")
	List<ClubServiceEntity> findActiveBySearch(@Param("searchText") String searchText,
			@Param("language") String language);

	@Query("SELECT DISTINCT c FROM ClubServiceEntity c JOIN c.events e WHERE c.publicationStatus = true AND e.deleted = false AND e.isActive = true")
	List<ClubServiceEntity> findActiveWithEvents();

	@Query("SELECT c FROM ClubServiceEntity c " +
			"LEFT JOIN FETCH c.locales loc " +
			"WHERE c.id = :id AND c.publicationStatus = true " +
			"AND (:language IS NULL OR LOWER(loc.language) IN (LOWER(:language), 'en'))")
	Optional<ClubServiceEntity> findDetailById(@Param("id") Long id, @Param("language") String language);

	@Query("SELECT c FROM ClubServiceEntity c WHERE c.user.id = :providerId")
	Optional<ClubServiceEntity> findByProviderId(Long providerId);

	@Query("SELECT c.id as id, c.name as name, c.description as description, c.locales as locales, "
			+ "c.maxCapacity as capacity, c.events as events, c.basePrice as basePrice, c.images as images, "
			+ "c.publicationStatus as publicationStatus "
			+ "FROM ClubServiceEntity c WHERE c.id = :id AND c.user.id = :providerId")
	Optional<ClubDetailProjection> findDetailByIdAndProvider(Long id, Long providerId);

	/*
	 * @Query("SELECT c.id as id, c.name as name, c.publicationStatus as publicationStatus, "
	 * +
	 * "c.user.name as providerName, c.maxCapacity as capacity, c.creationDate as creationDate "
	 * + "FROM ClubServiceEntity c")
	 * List<ClubManagementProjection> findAllForAdmin();
	 */

	@Query("SELECT c.id as id, c.name as name, c.publicationStatus as publicationStatus, "
			+ "c.user.name as providerName, c.creationDate as creationDate "
			+ "FROM ClubServiceEntity c WHERE c.publicationStatus = false")
	List<ClubManagementProjection> findPendingApproval();

	boolean existsByIdAndPublicationStatusTrue(Long id);

	@Query("SELECT COUNT(c) FROM ClubServiceEntity c WHERE c.user.id = :providerId AND c.publicationStatus = true")
	Long countActiveByProvider(Long providerId);

	@Query("SELECT c.id as id, c.name as name, c.description as description, "
			+ "c.basePrice as basePrice, c.locales as locales, c.images as images "
			+ "FROM ClubServiceEntity c WHERE c.publicationStatus = true AND c.maxCapacity >= :minCapacity")
	List<ClubSummaryProjection> findActiveByMinCapacity(Integer minCapacity);

	List<ClubServiceEntity> findByUser(User user);

	@Query("SELECT new us.hogu.controller.dto.response.ClubInfoStatsDto(" +
			"   c.id, " +
			"   c.name, " +
			"   c.description, " +
			"   (SELECT COUNT(b) FROM ClubBooking b WHERE b.clubService = c), " +
			"   (SELECT COALESCE(SUM(b.totalAmount), 0) FROM ClubBooking b WHERE b.clubService = c AND (b.status = us.hogu.model.enums.BookingStatus.COMPLETED OR b.status = us.hogu.model.enums.BookingStatus.CANCELLED_BY_PROVIDER)) "
			+
			") " +
			"FROM ClubServiceEntity c " +
			"WHERE c.user.id = :providerId")
	ClubInfoStatsDto getInfoStatsByProviderId(@Param("providerId") Long providerId);

	@Query("SELECT e FROM EventClubServiceEntity e WHERE e.id = :eventId AND e.deleted = false")
	Optional<EventClubServiceEntity> findByEventsId(@Param("eventId") Long eventId);

}
