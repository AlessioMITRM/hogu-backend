package us.hogu.repository.jpa;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import us.hogu.controller.dto.response.InfoStatsDto;
import us.hogu.model.ClubBooking;
import us.hogu.model.enums.BookingStatus;

public interface ClubBookingJpa extends JpaRepository<ClubBooking, Long> {
    
    // CLIENTE - Prenotazioni club dell'utente (dal JSON: GET /bookings?userId=)
    @Query("SELECT cb FROM ClubBooking cb WHERE cb.user.id = :userId")
    Page<ClubBooking> findByUserId(Long userId, Pageable pageable);
    
    // CLIENTE - Prenotazioni con stato specifico
    @Query("SELECT cb FROM ClubBooking cb WHERE cb.user.id = :userId AND cb.status = :status")
    List<ClubBooking> findByUserIdAndStatus(Long userId, BookingStatus status);
    
    // FORNITORE - Prenotazioni ricevute per un club (dal JSON: GET /bookings?providerId=)
    @Query("SELECT cb FROM ClubBooking cb WHERE cb.clubService.id = :clubServiceId")
    Page<ClubBooking> findByClubServiceId(Long clubServiceId, Pageable pageable);
    
    // FORNITORE - Prenotazioni con stato specifico
    @Query("SELECT cb FROM ClubBooking cb WHERE cb.clubService.id = :clubServiceId "
    		+ "AND cb.status = :status ")
    Page<ClubBooking> findByclubServiceIdAndStatus(Long clubServiceId, BookingStatus status, Pageable pageable);
    
    // ADMIN - Tutte le prenotazioni club
    @Query("SELECT cb FROM ClubBooking cb")
    List<ClubBooking> findAllClubBookings();
    
    // Per controllo esistenza e autorizzazioni
    boolean existsByIdAndUserId(Long id, Long userId);
    boolean existsByIdAndClubServiceUserId(Long id, Long providerId);
    
    // Ricerca per periodo
    @Query("SELECT cb FROM ClubBooking cb WHERE cb.clubService.id = :clubServiceId AND cb.reservationTime BETWEEN :startDate AND :endDate")
    List<ClubBooking> findByClubServiceAndDateRange(Long clubServiceId, OffsetDateTime startDate, OffsetDateTime endDate);
    
    // Prenotazioni per data specifica
    @Query("SELECT cb FROM ClubBooking cb WHERE cb.clubService.id = :clubServiceId AND DATE(cb.reservationTime) = :date")
    List<ClubBooking> findByClubServiceAndDate(Long clubServiceId, LocalDate date);
    
    // Prenotazioni per evento specifico
    @Query("SELECT cb FROM ClubBooking cb WHERE cb.clubService.id = :clubServiceId AND cb.specialRequests LIKE %:eventName%")
    List<ClubBooking> findByClubServiceAndEvent(Long clubServiceId, String eventName);
    
    // Statistiche fornitore
    @Query("SELECT COUNT(cb) FROM ClubBooking cb WHERE cb.clubService.user.id = :providerId")
    Long countByProviderId(Long providerId);
    
    @Query("SELECT COUNT(cb) FROM ClubBooking cb WHERE cb.clubService.user.id = :providerId AND cb.status = :status")
    Long countByProviderIdAndStatus(Long providerId, BookingStatus status);
    
    // Per dashboard fornitore - prenotazioni recenti
    @Query("SELECT cb FROM ClubBooking cb WHERE cb.clubService.user.id = :providerId ORDER BY cb.creationDate DESC")
    List<ClubBooking> findRecentByProviderId(Long providerId);
    
    // Per verificare conflitti di prenotazione (club)
    @Query("SELECT cb FROM ClubBooking cb WHERE cb.clubService.id = :clubServiceId AND cb.reservationTime = :reservationTime AND cb.status IN :activeStatuses")
    List<ClubBooking> findConflictingBookings(Long clubServiceId, OffsetDateTime reservationTime, List<BookingStatus> activeStatuses);
    
    // Prenotazioni per numero di persone (utile per statistiche)
    @Query("SELECT cb FROM ClubBooking cb WHERE cb.clubService.id = :clubServiceId AND cb.numberOfPeople >= :minPeople")
    List<ClubBooking> findByClubServiceAndMinPeople(Long clubServiceId, Integer minPeople);
    
    // Prenotazioni con richieste speciali (per eventi)
    @Query("SELECT cb FROM ClubBooking cb WHERE cb.clubService.id = :clubServiceId AND cb.specialRequests IS NOT NULL")
    List<ClubBooking> findByClubServiceWithSpecialRequests(Long clubServiceId);
    
    // Per trovare prenotazioni in una fascia oraria
    @Query("SELECT cb FROM ClubBooking cb WHERE cb.clubService.id = :clubServiceId AND " +
           "EXTRACT(HOUR FROM cb.reservationTime) BETWEEN :startHour AND :endHour")
    List<ClubBooking> findByClubServiceAndTimeRange(Long clubServiceId, Integer startHour, Integer endHour);
    
    // Prenotazioni per mese (per statistiche)
    @Query("SELECT cb FROM ClubBooking cb WHERE cb.clubService.id = :clubServiceId AND " +
           "EXTRACT(MONTH FROM cb.reservationTime) = :month AND EXTRACT(YEAR FROM cb.reservationTime) = :year")
    List<ClubBooking> findByClubServiceAndMonth(Long clubServiceId, Integer month, Integer year);

}
