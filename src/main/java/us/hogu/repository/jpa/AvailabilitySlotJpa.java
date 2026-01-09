package us.hogu.repository.jpa;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import us.hogu.model.AvailabilitySlot;
import us.hogu.model.enums.ServiceType;

public interface AvailabilitySlotJpa extends JpaRepository<AvailabilitySlot, Long> {
    
    List<AvailabilitySlot> findByServiceTypeAndServiceIdAndDate(
        ServiceType serviceType, Long serviceId, LocalDate date);
    
    List<AvailabilitySlot> findByServiceTypeAndServiceIdAndStartTimeBetween(
        ServiceType serviceType, Long serviceId, OffsetDateTime start, OffsetDateTime end);
    
    // Per controllare disponibilità
    @Query("SELECT a FROM AvailabilitySlot a WHERE a.serviceType = :serviceType AND a.serviceId = :serviceId " +
           "AND a.date = :date AND a.availableSlots > 0")
    List<AvailabilitySlot> findAvailableSlots(
        ServiceType serviceType, Long serviceId, LocalDate date);
}
