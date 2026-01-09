package us.hogu.repository.jpa;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import us.hogu.model.NccBooking;
import us.hogu.model.enums.BookingStatus;
import us.hogu.repository.projection.NccBookingProjection;

public interface NccBookingJpa extends JpaRepository<NccBooking, Long> {
    
    // CLIENTE - Prenotazioni NCC dell'utente (dal JSON: GET /bookings?userId=)
    @Query("SELECT nb FROM NccBooking nb WHERE nb.user.id = :userId")
    Page<NccBooking> findByUserId(Long userId, Pageable pageable);
    
    // CLIENTE - Prenotazioni con stato specifico
    @Query("SELECT nb FROM NccBooking nb WHERE nb.user.id = :userId AND nb.status = :status")
    List<NccBooking> findByUserIdAndStatus(Long userId, BookingStatus status);
    
    // FORNITORE - Prenotazioni ricevute per un servizio NCC (dal JSON: GET /bookings?providerId=)
    @Query("SELECT nb FROM NccBooking nb WHERE nb.nccService.id = :nccServiceId")
    List<NccBooking> findByNccServiceId(Long nccServiceId);
    
    // FORNITORE - Prenotazioni con stato specifico
    @Query("SELECT nb FROM NccBooking nb WHERE nb.nccService.id = :nccServiceId AND nb.status = :status")
    List<NccBooking> findByNccServiceIdAndStatus(Long nccServiceId, BookingStatus status);
    
    // ADMIN - Tutte le prenotazioni NCC
    @Query("SELECT nb FROM NccBooking nb")
    List<NccBooking> findAllNccBookings();
    
    // Per controllo esistenza e autorizzazioni
    boolean existsByIdAndUserId(Long id, Long userId);
    boolean existsByIdAndNccServiceUserId(Long id, Long providerId);
    
    // Ricerca per periodo
    @Query("SELECT nb FROM NccBooking nb WHERE nb.nccService.id = :nccServiceId AND nb.pickupTime BETWEEN :startDate AND :endDate")
    List<NccBooking> findByNccServiceAndDateRange(Long nccServiceId, OffsetDateTime startDate, OffsetDateTime endDate);
    
    // Prenotazioni per data specifica
    @Query("SELECT nb FROM NccBooking nb WHERE nb.nccService.id = :nccServiceId AND DATE(nb.pickupTime) = :date")
    List<NccBooking> findByNccServiceAndDate(Long nccServiceId, LocalDate date);
    
    // Statistiche fornitore
    @Query("SELECT COUNT(nb) FROM NccBooking nb WHERE nb.nccService.user.id = :providerId")
    Long countByProviderId(Long providerId);
    
    @Query("SELECT COUNT(nb) FROM NccBooking nb WHERE nb.nccService.user.id = :providerId AND nb.status = :status")
    Long countByProviderIdAndStatus(Long providerId, BookingStatus status);
    
    // Per dashboard fornitore - prenotazioni recenti
    @Query("SELECT nb FROM NccBooking nb WHERE nb.nccService.user.id = :providerId ORDER BY nb.creationDate DESC")
    List<NccBooking> findRecentByProviderId(Long providerId);
    
    // Per verificare conflitti di prenotazione (NCC)
    @Query("SELECT nb FROM NccBooking nb WHERE nb.nccService.id = :nccServiceId AND nb.pickupTime = :pickupTime AND nb.status IN :activeStatuses")
    List<NccBooking> findConflictingBookings(Long nccServiceId, OffsetDateTime pickupTime, List<BookingStatus> activeStatuses);
    
    // Prenotazioni per destinazione (utile per statistiche)
    @Query("SELECT nb FROM NccBooking nb WHERE nb.nccService.id = :nccServiceId AND LOWER(nb.destination) LIKE LOWER(CONCAT('%', :destination, '%'))")
    List<NccBooking> findByNccServiceAndDestination(Long nccServiceId, String destination);
    
    // Per trovare prenotazioni in un'area geografica
    @Query("SELECT DISTINCT nb FROM NccBooking nb " +
        "JOIN nb.nccService n " +
        "JOIN n.locales loc " +
        "WHERE (:area IS NULL OR LOWER(loc.city) LIKE LOWER(CONCAT('%', :area, '%')))")
    List<NccBooking> findByServiceArea(@Param("area") String area);
    
	// Query con projection nel repository:
	@Query("SELECT nb.id as id, nb.pickupTime as pickupTime, nb.pickupLocation as pickupLocation, " +
	       "nb.destination as destination, nb.status as status, nb.totalAmount as totalAmount, " +
	       "nb.creationDate as creationDate, nb.user.name as customerName, nb.user.email as customerEmail, " +
	       "nb.nccService.name as nccServiceName " +
	       "FROM NccBooking nb WHERE nb.nccService.id = :nccServiceId")
	List<NccBookingProjection> findProjectionsByNccServiceId(Long nccServiceId);
	
}
