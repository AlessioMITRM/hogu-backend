package us.hogu.repository.jpa;

import java.util.List;

import us.hogu.model.Notification;

public interface NotificationJpa {
    List<Notification> findByUserIdOrderByCreationDateDesc(Long userId);
    
    List<Notification> findByUserIdAndReadFalse(Long userId);
    
    Long countByUserIdAndReadFalse(Long userId);
}
