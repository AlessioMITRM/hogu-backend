package us.hogu.repository.jpa;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import us.hogu.model.Booking;
import us.hogu.model.enums.BookingStatus;
import java.math.BigDecimal;

public interface BookingJpa extends JpaRepository<Booking, Long> {

    // Trova tutte le prenotazioni con paginazione
    Page<Booking> findAll(Pageable pageable);

    // Ricerca per codice prenotazione con paginazione
    Page<Booking> findByBookingCodeContainingIgnoreCase(String bookingCode, Pageable pageable);

    @Query("SELECT b FROM Booking b " +
            "LEFT JOIN NccBooking nb ON b.id = nb.id " +
            "LEFT JOIN nb.nccService ns " +
            "LEFT JOIN ns.user nsu " +
            "LEFT JOIN BnbBooking bb ON b.id = bb.id " +
            "LEFT JOIN bb.bnbService bs " +
            "LEFT JOIN bs.user bsu " +
            "LEFT JOIN ClubBooking cb ON b.id = cb.id " +
            "LEFT JOIN cb.clubService cs " +
            "LEFT JOIN cs.user csu " +
            "LEFT JOIN RestaurantBooking rb ON b.id = rb.id " +
            "LEFT JOIN rb.restaurantService rs " +
            "LEFT JOIN rs.user rsu " +
            "LEFT JOIN LuggageBooking lb ON b.id = lb.id " +
            "LEFT JOIN lb.luggageService ls " +
            "LEFT JOIN ls.user lsu " +
            "WHERE LOWER(b.bookingCode) LIKE LOWER(CONCAT('%', :search, '%')) " +
            "OR LOWER(nsu.email) LIKE LOWER(CONCAT('%', :search, '%')) " +
            "OR LOWER(bsu.email) LIKE LOWER(CONCAT('%', :search, '%')) " +
            "OR LOWER(csu.email) LIKE LOWER(CONCAT('%', :search, '%')) " +
            "OR LOWER(rsu.email) LIKE LOWER(CONCAT('%', :search, '%')) " +
            "OR LOWER(lsu.email) LIKE LOWER(CONCAT('%', :search, '%'))")
    Page<Booking> searchByCodeOrProvider(@Param("search") String search, Pageable pageable);

    // Trova la prima prenotazione (più recente) in attesa di pagamento
    Optional<Booking> findFirstByUserIdAndStatusOrderByCreationDateDesc(Long userId, BookingStatus status);

    List<Booking> findByUserIdAndStatus(Long userId, BookingStatus status);

    List<Booking> findByUserId(Long userId);

    @Query("SELECT COALESCE(SUM(b.totalAmount), 0) FROM Booking b WHERE b.status IN :statuses")
    BigDecimal calculateTotalRevenueByStatuses(@Param("statuses") List<BookingStatus> statuses);

    long countByUserId(Long userId);
}
