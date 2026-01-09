package us.hogu.repository.jpa;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import us.hogu.model.AvailabilitySlot;
import us.hogu.model.BnbRoomPriceCalendar;

public interface BnbRoomPriceCalendarJpa extends JpaRepository<BnbRoomPriceCalendar, Long> {
    
	List<BnbRoomPriceCalendar> findByRoomId(Long roomId);

}
