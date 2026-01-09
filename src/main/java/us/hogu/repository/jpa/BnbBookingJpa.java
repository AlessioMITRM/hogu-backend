package us.hogu.repository.jpa;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import us.hogu.model.BnbBooking;

public interface BnbBookingJpa extends JpaRepository<BnbBooking, Long> {
		
	List<BnbBooking> findByUserId(Long userId);

	Page<BnbBooking> findByUserId(Long userId, Pageable pageable);

	Page<BnbBooking> findByBnbServiceId(Long bnbServiceId, Pageable pageable);
}
