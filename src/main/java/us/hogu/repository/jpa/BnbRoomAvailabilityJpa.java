package us.hogu.repository.jpa;

import java.time.LocalDate;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import us.hogu.model.BnbRoomAvailability;

@Repository
public interface BnbRoomAvailabilityJpa extends JpaRepository<BnbRoomAvailability, Long> {
    Optional<BnbRoomAvailability> findByRoomIdAndDate(Long roomId, LocalDate date);
}
