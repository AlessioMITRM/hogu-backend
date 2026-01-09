package us.hogu.repository.jpa;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import us.hogu.model.RestaurantBooking;
import us.hogu.model.enums.BookingStatus;

public interface RestaurantBookingJpa extends JpaRepository<RestaurantBooking, Long> {
    
    @Query("SELECT rb FROM RestaurantBooking rb WHERE rb.user.id = :userId")
    Page<RestaurantBooking> findByUserId(Long userId, Pageable pageable);
    
    // CLIENTE - Prenotazioni dell'utente con stato specifico
    @Query("SELECT rb FROM RestaurantBooking rb WHERE rb.user.id = :userId AND rb.status = :status")
    List<RestaurantBooking> findByUserIdAndStatus(Long userId, BookingStatus status);
    
    // FORNITORE - Prenotazioni ricevute per un ristorante (dal JSON: GET /bookings?providerId=)
    @Query("SELECT rb FROM RestaurantBooking rb WHERE rb.restaurantService.id = :restaurantId")
    Page<RestaurantBooking> findByRestaurantServiceId(Long restaurantId, Pageable pageable);
    
    // FORNITORE - Prenotazioni ricevute con stato specifico
    @Query("SELECT rb FROM RestaurantBooking rb WHERE rb.restaurantService.id = :restaurantId AND rb.status = :status")
    List<RestaurantBooking> findByRestaurantServiceIdAndStatus(Long restaurantId, BookingStatus status);
    
    // ADMIN - Tutte le prenotazioni ristorante
    @Query("SELECT rb FROM RestaurantBooking rb")
    List<RestaurantBooking> findAllRestaurantBookings();
    
    // Per controllo esistenza e autorizzazioni
    boolean existsByIdAndUserId(Long id, Long userId);
    boolean existsByIdAndRestaurantServiceUserId(Long id, Long providerId);
    
    // Ricerca per data/periodo
    @Query("SELECT rb FROM RestaurantBooking rb WHERE rb.restaurantService.id = :restaurantId AND rb.reservationTime BETWEEN :startDate AND :endDate")
    List<RestaurantBooking> findByRestaurantAndDateRange(Long restaurantId, OffsetDateTime startDate, OffsetDateTime endDate);
    
    // Prenotazioni per data specifica
    @Query("SELECT rb FROM RestaurantBooking rb WHERE rb.restaurantService.id = :restaurantId AND DATE(rb.reservationTime) = :date")
    List<RestaurantBooking> findByRestaurantAndDate(Long restaurantId, LocalDate date);
    
    // Statistiche fornitore
    @Query("SELECT COUNT(rb) FROM RestaurantBooking rb WHERE rb.restaurantService.user.id = :providerId")
    Long countByProviderId(Long providerId);
    
    @Query("SELECT COUNT(rb) FROM RestaurantBooking rb WHERE rb.restaurantService.user.id = :providerId AND rb.status = :status")
    Long countByProviderIdAndStatus(Long providerId, BookingStatus status);
    
    // Per dashboard fornitore - prenotazioni recenti
    @Query("SELECT rb FROM RestaurantBooking rb WHERE rb.restaurantService.user.id = :providerId ORDER BY rb.creationDate DESC")
    List<RestaurantBooking> findRecentByProviderId(Long providerId);
    
    // Per verificare conflitti di prenotazione
    @Query("SELECT rb FROM RestaurantBooking rb WHERE rb.restaurantService.id = :restaurantId AND rb.reservationTime = :reservationTime AND rb.status IN :activeStatuses")
    List<RestaurantBooking> findConflictingBookings(Long restaurantId, OffsetDateTime reservationTime, List<BookingStatus> activeStatuses);
    
    // Conta il numero totale di persone prenotate per un ristorante in una specifica data/ora
    @Query("SELECT COALESCE(SUM(rb.numberOfPeople), 0) FROM RestaurantBooking rb " +
           "WHERE rb.restaurantService.id = :restaurantId " +
           "AND rb.reservationTime = :reservationTime " +
           "AND rb.status IN ('PENDING', 'CONFIRMED')")
    Integer countBookedPeopleByRestaurantAndTime(Long restaurantId, OffsetDateTime reservationTime);
}
