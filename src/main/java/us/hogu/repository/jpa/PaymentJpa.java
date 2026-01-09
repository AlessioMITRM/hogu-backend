package us.hogu.repository.jpa;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import us.hogu.model.AvailabilitySlot;
import us.hogu.model.Payment;
import us.hogu.model.enums.PaymentStatus;

public interface PaymentJpa extends JpaRepository<Payment, Long> {
    Optional<Payment> findByBooking_Id(Long bookingId);
    
    List<Payment> findByUser_Id(Long userId);
        
    Optional<Payment> findByPaymentIdIntent(String paymentIntentId);
}
