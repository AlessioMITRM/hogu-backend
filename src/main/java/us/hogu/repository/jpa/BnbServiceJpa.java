package us.hogu.repository.jpa;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import us.hogu.model.BnbServiceEntity;
import us.hogu.model.User;

public interface BnbServiceJpa extends JpaRepository<BnbServiceEntity, Long> {
    
	Page<BnbServiceEntity> findByUserId(Long userId, Pageable pageable);

	List<BnbServiceEntity> findByUser(User user);

	Page<BnbServiceEntity> findByPublicationStatusTrue(Pageable pageable);
	
    List<BnbServiceEntity> findByPublicationStatus(Boolean publicationStatus);
}
