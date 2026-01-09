package us.hogu.repository.jpa;

import java.time.OffsetDateTime;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import us.hogu.model.EventClubServiceEntity;

public interface EventClubServiceRepository extends JpaRepository<EventClubServiceEntity, Long> {
    
    List<EventClubServiceEntity> findByclubServiceIdAndIsActiveTrue(Long clubServiceId);
    
    List<EventClubServiceEntity> findByclubServiceIdAndStartTimeAfter(Long clubServiceId, OffsetDateTime startTime);
    
    List<EventClubServiceEntity> findByclubServiceIdAndStartTimeBetween(
        Long clubServiceId, OffsetDateTime start, OffsetDateTime end);
    
    @Query("SELECT e FROM EventClubServiceEntity e WHERE e.clubService.id = :clubServiceId AND e.isActive = true AND e.startTime > :now ORDER BY e.startTime ASC")
    List<EventClubServiceEntity> findUpcomingEvents(@Param("clubServiceId") Long clubServiceId, @Param("now") OffsetDateTime now);
    
    @Query("SELECT e FROM EventClubServiceEntity e WHERE e.startTime BETWEEN :start AND :end AND e.isActive = true")
    List<EventClubServiceEntity> findEventsByDateRange(@Param("start") OffsetDateTime start, @Param("end") OffsetDateTime end);
    
    @Query("SELECT DISTINCT e FROM EventClubServiceEntity e " +
    	       "JOIN e.locales l " +
    	       "WHERE e.isActive = true " +
    	       "AND (:city IS NULL OR LOWER(l.city) LIKE LOWER(CONCAT('%', :city, '%'))) " +
    	       "AND (:state IS NULL OR LOWER(l.state) LIKE LOWER(CONCAT('%', :state, '%'))) " +
    	       "AND (:eventType IS NULL OR e.theme = :eventType) " +
    	       "AND (:startDate IS NULL OR DATE(e.startTime) = DATE(:startDate))")
	Page<EventClubServiceEntity> findByClubIdWithFilters(
	    @Param("city") String city,
	    @Param("state") String state,
	    @Param("eventType") String eventType,
	    @Param("startDate") OffsetDateTime startDate,
	    Pageable pageable
	);
}
