package us.hogu.repository.jpa;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import us.hogu.model.BnbRoom;

public interface BnbRoomJpa extends JpaRepository<BnbRoom, Long> {

	List<BnbRoom> findByBnbServiceId(Long bnbServiceId);
	
    @Query("SELECT DISTINCT r FROM BnbRoom r " +
           "JOIN r.bnbService bs " +
           "LEFT JOIN bs.locales sl " +
           "WHERE " +
           // --- RICERCA TESTUALE GENERICA ---
           // Usiamo CAST per forzare Hibernate a trattarli come stringhe
           "(:searchTerm IS NULL OR LOWER(CAST(r.name AS string)) LIKE LOWER(CONCAT('%', :searchTerm, '%')) " +
           "   OR LOWER(CAST(sl.address AS string)) LIKE LOWER(CONCAT('%', :searchTerm, '%'))) " +
           "AND " +
           // --- RICERCA GEOGRAFICA ---
           "(:city IS NULL OR LOWER(CAST(sl.city AS string)) LIKE LOWER(CONCAT('%', :city, '%'))) " +
           "AND " +
           // *** QUI C'ERA L'ERRORE: FORZIAMO IL CAST ANCHE SU STATE ***
           "(:state IS NULL OR LOWER(CAST(sl.state AS string)) LIKE LOWER(CONCAT('%', :state, '%'))) " +
           "AND " +
           // --- DISPONIBILITÀ DATE ---
           "(:checkIn IS NULL OR NOT EXISTS (" +
           "   SELECT b FROM BnbBooking b " +
           "   WHERE b.room = r " +
           "   AND b.status NOT IN (us.hogu.model.enums.BookingStatus.CANCELLED_BY_PROVIDER, " +
           "                        us.hogu.model.enums.BookingStatus.CANCELLED_BY_ADMIN, " +
           "                        us.hogu.model.enums.BookingStatus.REFUNDED_BY_ADMIN) " +
           "   AND b.checkInDate < :checkOut AND b.checkOutDate > :checkIn)) " + 
           "AND " +
           // --- OSPITI ---
           "(r.maxGuests >= :adults + :children)")
    Page<BnbRoom> searchBnbRooms(
        @Param("searchTerm") String searchTerm, 
        @Param("city") String city,       
        @Param("state") String state,     
        @Param("checkIn") LocalDate checkIn, 
        @Param("checkOut") LocalDate checkOut, 
        @Param("adults") Integer adults, 
        @Param("children") Integer children, 
        Pageable pageable
    );
}