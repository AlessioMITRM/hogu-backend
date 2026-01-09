package us.hogu.repository.jpa;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import us.hogu.model.LuggageBooking;
import us.hogu.model.enums.BookingStatus;

public interface LuggageBookingJpa extends JpaRepository<LuggageBooking, Long> {
    
    // CLIENTE - Prenotazioni bagagli dell'utente
    @Query("SELECT lb FROM LuggageBooking lb WHERE lb.user.id = :userId")
    Page<LuggageBooking> findByUserId(@Param("userId") Long userId, Pageable pageable);
    
    // CLIENTE - Prenotazioni con stato specifico
    @Query("SELECT lb FROM LuggageBooking lb WHERE lb.user.id = :userId AND lb.status = :status")
    List<LuggageBooking> findByUserIdAndStatus(@Param("userId") Long userId, @Param("status") BookingStatus status);
    
    // FORNITORE - Prenotazioni ricevute per un servizio bagagli
    Page<LuggageBooking> findByLuggageServiceId(Long luggageServiceId, Pageable pageable);

    // FORNITORE - Prenotazioni con stato specifico
    @Query("SELECT lb FROM LuggageBooking lb WHERE lb.luggageService.id = :luggageServiceId AND lb.status = :status")
    List<LuggageBooking> findByLuggageServiceIdAndStatus(@Param("luggageServiceId") Long luggageServiceId,
                                                         @Param("status") BookingStatus status);
    
    // ADMIN - Tutte le prenotazioni
    @Query("SELECT lb FROM LuggageBooking lb")
    List<LuggageBooking> findAllLuggageBookings();
    
    // Per controllo esistenza e autorizzazioni
    boolean existsByIdAndUserId(Long id, Long userId);
    boolean existsByIdAndLuggageServiceUserId(Long id, Long providerId);
    
    // Ricerca per periodo
    @Query("SELECT lb FROM LuggageBooking lb WHERE lb.luggageService.id = :luggageServiceId AND lb.dropOffTime BETWEEN :startDate AND :endDate")
    List<LuggageBooking> findByLuggageServiceAndDateRange(@Param("luggageServiceId") Long luggageServiceId,
                                                          @Param("startDate") OffsetDateTime startDate,
                                                          @Param("endDate") OffsetDateTime endDate);
    
    // Prenotazioni per data specifica
    @Query("SELECT lb FROM LuggageBooking lb WHERE lb.luggageService.id = :luggageServiceId AND DATE(lb.dropOffTime) = :date")
    List<LuggageBooking> findByLuggageServiceAndDate(@Param("luggageServiceId") Long luggageServiceId,
                                                     @Param("date") LocalDate date);
    
    // Prenotazioni per tipo di bagaglio
    @Query("SELECT lb FROM LuggageBooking lb WHERE lb.luggageService.id = :luggageServiceId AND lb.specialRequests LIKE %:luggageType%")
    List<LuggageBooking> findByLuggageServiceAndLuggageType(@Param("luggageServiceId") Long luggageServiceId,
                                                            @Param("luggageType") String luggageType);
    
    // Statistiche fornitore
    @Query("SELECT COUNT(lb) FROM LuggageBooking lb WHERE lb.luggageService.user.id = :providerId")
    Long countByProviderId(@Param("providerId") Long providerId);
    
    @Query("SELECT COUNT(lb) FROM LuggageBooking lb WHERE lb.luggageService.user.id = :providerId AND lb.status = :status")
    Long countByProviderIdAndStatus(@Param("providerId") Long providerId, @Param("status") BookingStatus status);
    
    // Prenotazioni recenti
    @Query("SELECT lb FROM LuggageBooking lb WHERE lb.luggageService.user.id = :providerId ORDER BY lb.creationDate DESC")
    List<LuggageBooking> findRecentByProviderId(@Param("providerId") Long providerId);
    
    // Prenotazioni con conflitto orario
    @Query("SELECT lb FROM LuggageBooking lb WHERE lb.luggageService.id = :luggageServiceId AND lb.dropOffTime = :dropOffTime AND lb.status IN :activeStatuses")
    List<LuggageBooking> findConflictingBookings(@Param("luggageServiceId") Long luggageServiceId,
                                                 @Param("dropOffTime") OffsetDateTime dropOffTime,
                                                 @Param("activeStatuses") List<BookingStatus> activeStatuses);
    
    // Prenotazioni per numero minimo di bagagli
    @Query("SELECT lb FROM LuggageBooking lb WHERE lb.luggageService.id = :luggageServiceId")
    List<LuggageBooking> findByLuggageServiceAndMinLuggageCount(@Param("luggageServiceId") Long luggageServiceId);
    
    // Prenotazioni con richieste speciali
    @Query("SELECT lb FROM LuggageBooking lb WHERE lb.luggageService.id = :luggageServiceId AND lb.specialRequests IS NOT NULL")
    List<LuggageBooking> findByLuggageServiceWithSpecialRequests(@Param("luggageServiceId") Long luggageServiceId);
    
    // Prenotazioni per fascia oraria
    @Query("SELECT lb FROM LuggageBooking lb WHERE lb.luggageService.id = :luggageServiceId AND EXTRACT(HOUR FROM lb.dropOffTime) BETWEEN :startHour AND :endHour")
    List<LuggageBooking> findByLuggageServiceAndTimeRange(@Param("luggageServiceId") Long luggageServiceId,
                                                          @Param("startHour") Integer startHour,
                                                          @Param("endHour") Integer endHour);
    
    // Prenotazioni a lungo termine
    @Query("SELECT lb FROM LuggageBooking lb WHERE lb.luggageService.id = :luggageServiceId AND lb.specialRequests LIKE '%giorn%'")
    List<LuggageBooking> findByLuggageServiceAndLongTerm(@Param("luggageServiceId") Long luggageServiceId);
    
    // Prenotazioni attive
    @Query("SELECT lb FROM LuggageBooking lb WHERE lb.luggageService.id = :luggageServiceId AND lb.dropOffTime <= :currentTime AND lb.status IN :activeStatuses")
    List<LuggageBooking> findActiveBookings(@Param("luggageServiceId") Long luggageServiceId,
                                            @Param("currentTime") OffsetDateTime currentTime,
                                            @Param("activeStatuses") List<BookingStatus> activeStatuses);
    
    // Prenotazioni per zona/area
    @Query("SELECT DISTINCT lb FROM LuggageBooking lb JOIN lb.luggageService ls JOIN ls.locales loc " +
           "WHERE (:area IS NULL OR LOWER(loc.city) LIKE LOWER(CONCAT('%', :area, '%')) " +
           "OR LOWER(loc.address) LIKE LOWER(CONCAT('%', :area, '%')))")
    List<LuggageBooking> findByServiceArea(@Param("area") String area);
}
