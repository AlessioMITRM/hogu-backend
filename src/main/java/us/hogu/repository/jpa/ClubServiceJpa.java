package us.hogu.repository.jpa;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import us.hogu.controller.dto.response.InfoStatsDto;
import us.hogu.model.ClubServiceEntity;
import us.hogu.model.EventClubServiceEntity;
import us.hogu.model.User;
import us.hogu.repository.projection.ClubDetailProjection;
import us.hogu.repository.projection.ClubManagementProjection;
import us.hogu.repository.projection.ClubSummaryProjection;

public interface ClubServiceJpa extends JpaRepository<ClubServiceEntity, Long> {

    boolean existsByIdAndUserId(Long id, Long userId);
	
    Optional<ClubServiceEntity> findById(Long id);
	    
    @Query("SELECT e FROM EventClubServiceEntity e WHERE e.clubService.id = :clubId")
    Page<EventClubServiceEntity> findByClubServiceId(@Param("clubId") Long clubId, Pageable pageable);
    
	@Query("SELECT e FROM EventClubServiceEntity e " +
		       "JOIN e.clubService cs " +
		       "WHERE cs.id = :clubId AND e.id = :eventId")
	Optional<EventClubServiceEntity> findEventByClubIdAndEventId(
	    @Param("clubId") Long clubId,
	    @Param("eventId") Long eventId
	);
	
	@Query(
		    "SELECT e " +
		    "FROM EventClubServiceEntity e " +
		    "WHERE e.clubService.id = :clubId " +
		    "AND e.startTime >= :startOfDay " +
		    "AND e.startTime < :endOfDay"
		)
		Page<EventClubServiceEntity> findTodayEventsByClubServiceId(
		        @Param("clubId") Long clubId,
		        @Param("startOfDay") OffsetDateTime startOfDay,
		        @Param("endOfDay") OffsetDateTime endOfDay,
		        Pageable pageable
		);
    
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
		"           AND LOWER(sl.city) LIKE LOWER(CONCAT('%', :searchText, '%')) " +
		"     )" +
		")")
	List<ClubServiceEntity> findActiveBySearch(@Param("searchText") String searchText);

    @Query("SELECT c FROM ClubServiceEntity c WHERE c.publicationStatus = true AND c.events IS NOT EMPTY")
    List<ClubServiceEntity> findActiveWithEvents();

	@Query("SELECT c.id as id, c.name as name, c.description as description, c.locales as locales, "
			+ "c.maxCapacity as capacity, c.events as events, c.basePrice as basePrice, c.images as images, "
			+ "c.user.id as providerId, c.user.name as providerName "
			+ "FROM ClubServiceEntity c WHERE c.id = :id AND c.publicationStatus = true")
	Optional<ClubDetailProjection> findDetailById(Long id);

	@Query("SELECT c FROM ClubServiceEntity c WHERE c.user.id = :providerId")
	Optional<ClubServiceEntity> findByProviderId(Long providerId);
	
	@Query("SELECT c.id as id, c.name as name, c.description as description, c.locales as locales, "
			+ "c.maxCapacity as capacity, c.events as events, c.basePrice as basePrice, c.images as images, "
			+ "c.publicationStatus as publicationStatus "
			+ "FROM ClubServiceEntity c WHERE c.id = :id AND c.user.id = :providerId")
	Optional<ClubDetailProjection> findDetailByIdAndProvider(Long id, Long providerId);

	/*@Query("SELECT c.id as id, c.name as name, c.publicationStatus as publicationStatus, "
			+ "c.user.name as providerName, c.maxCapacity as capacity, c.creationDate as creationDate "
			+ "FROM ClubServiceEntity c")
	List<ClubManagementProjection> findAllForAdmin();*/

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
	
	@Query(
		    "SELECT new us.hogu.controller.dto.response.InfoStatsDto(" +
		    "   c.id, " +
		    "   (SELECT COUNT(b) FROM ClubBooking b WHERE b.clubService = c), " +
		    "   (SELECT COALESCE(SUM(b.totalAmount), 0) FROM ClubBooking b WHERE b.clubService = c) " +
		    ") " +
		    "FROM ClubServiceEntity c " +
		    "WHERE c.user.id = :providerId"
		)
		InfoStatsDto getInfoStatsByProviderId(@Param("providerId") Long providerId);

	@Query("SELECT e FROM EventClubServiceEntity e WHERE e.id = :eventId")
	Optional<EventClubServiceEntity> findByEventsId(@Param("eventId") Long eventId);


}
