package us.hogu.repository.jpa;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import us.hogu.model.EventPricingConfiguration;

@Repository
public interface EventPricingConfigurationJpa extends JpaRepository<EventPricingConfiguration, Long> {
    
    List<EventPricingConfiguration> findByEventClubServiceIdAndIsActiveTrue(Long eventClubServiceId);
    
    @Query("SELECT epc FROM EventPricingConfiguration epc WHERE epc.eventClubService.id = :eventId AND epc.pricingType = :pricingType AND epc.isActive = true")
    EventPricingConfiguration findByEventIdAndPricingType(@Param("eventId") Long eventId, @Param("pricingType") String pricingType);
    
    List<EventPricingConfiguration> findByEventClubServiceId(Long eventClubServiceId);
}