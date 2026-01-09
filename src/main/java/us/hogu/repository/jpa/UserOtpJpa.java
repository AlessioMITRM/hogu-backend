package us.hogu.repository.jpa;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import us.hogu.model.User;
import us.hogu.model.UserOtp;


public interface UserOtpJpa extends JpaRepository<UserOtp, Long> {
	
    // Restituisce il primo OTP non verificato e non scaduto per un utente
	Optional<UserOtp> findFirstByUserAndVerifiedFalseAndExpirationDateAfterOrderByIdDesc(User user, OffsetDateTime dateTime);

	List<UserOtp> findByUser(User user);
}
