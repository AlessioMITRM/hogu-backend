package us.hogu.repository.jpa;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import us.hogu.model.AvailabilitySlot;
import us.hogu.model.BnbRoomPriceCalendar;

public interface BnbRoomPriceCalendarJpa extends JpaRepository<BnbRoomPriceCalendar, Long> {
    
	List<BnbRoomPriceCalendar> findByRoomId(Long roomId);
	
	@Query("SELECT p FROM BnbRoomPriceCalendar p " +
		       "WHERE p.room.id = :roomId " +
		       "  AND p.endDate   >= :from " +
		       "  AND p.startDate <= :to " +
		       "ORDER BY p.startDate")
	List<BnbRoomPriceCalendar> findOverlappingPriceRules(
	    @Param("roomId") Long roomId,
	    @Param("from") LocalDate from,
	    @Param("to") LocalDate to
	);

}
