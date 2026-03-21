package us.hogu.repository.jpa;

import java.util.Collection;
import java.util.List;
import java.time.LocalDate;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import us.hogu.model.BnbBooking;
import us.hogu.model.enums.BookingStatus;

public interface BnbBookingJpa extends JpaRepository<BnbBooking, Long> {
	Page<BnbBooking> findByUserId(Long userId, Pageable pageable);

	Optional<BnbBooking> findByBookingCode(String bookingCode);

	Page<BnbBooking> findByBnbServiceId(Long bnbServiceId, Pageable pageable);

	Page<BnbBooking> findByBnbServiceIdAndStatusIn(Long bnbServiceId, Iterable<BookingStatus> statuses,
			Pageable pageable);

	Page<BnbBooking> findByBnbServiceIdAndCheckInDate(Long bnbServiceId, LocalDate checkInDate, Pageable pageable);

	Page<BnbBooking> findByBnbServiceIdAndCheckInDateGreaterThanEqual(Long bnbServiceId, LocalDate checkInDate,
			Pageable pageable);

	Page<BnbBooking> findByBnbServiceIdAndCheckInDateLessThan(Long bnbServiceId, LocalDate checkInDate,
			Pageable pageable);

	@Query("SELECT bb FROM BnbBooking bb JOIN FETCH bb.room r WHERE bb.status IN :statuses")
	List<BnbBooking> findAllByStatusInWithRoom(@Param("statuses") Collection<BookingStatus> statuses);
}
