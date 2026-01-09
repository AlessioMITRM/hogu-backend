package us.hogu.repository.jpa;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import us.hogu.model.Notification;
import us.hogu.model.Review;
import us.hogu.model.enums.ServiceType;

public interface ReviewJpa extends  JpaRepository<Review, Long> {
    
    List<Review> findByServiceTypeAndServiceId(ServiceType serviceType, Long serviceId);
    
    List<Review> findByUserId(Long userId);
    
    // Per calcolare rating medio
    @Query("SELECT AVG(r.rating) FROM Review r WHERE r.serviceType = :serviceType AND r.serviceId = :serviceId")
    Double findAverageRatingByService(ServiceType serviceType, Long serviceId);
}
