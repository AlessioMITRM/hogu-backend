package us.hogu.repository.jpa;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import us.hogu.model.User;
import us.hogu.model.UserServiceVerification;
import us.hogu.model.enums.ServiceType;
import us.hogu.model.enums.VerificationStatusServiceEY;

public interface UserServiceVerificationJpa extends JpaRepository<UserServiceVerification, Long> {
	
    boolean existsByUserIdAndServiceTypeAndVerificationStatus(
            Long userId,
            ServiceType serviceType,
            VerificationStatusServiceEY verificationStatus
    );
	
	Optional<UserServiceVerification> findByUser(User user);
	
	List<UserServiceVerification> findByVerificationStatusAndUser(VerificationStatusServiceEY verificationStatus, User user);
	
}
