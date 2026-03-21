package us.hogu.repository.jpa;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import us.hogu.model.User;
import us.hogu.model.enums.UserRole;
import us.hogu.model.enums.UserStatus;
import us.hogu.model.internal.ProviderServicesCheck;
import us.hogu.repository.projection.UserProfileProjection;
import us.hogu.repository.projection.UserSummaryProjection;

public interface UserJpa extends JpaRepository<User, Long> {
	// Auth
	Optional<User> findByEmail(String email);

	boolean existsByEmail(String email);

	// Projection per liste
	@Query("SELECT u.id as id, u.name as name, u.surname as surname, u.email as email, u.role as role FROM User u WHERE u.role = :role")
	List<UserSummaryProjection> findByRole(UserRole role);

	@Query("SELECT u.id as id, u.name as name, u.surname as surname, u.email as email, u.creationDate as creationDate FROM User u")
	List<UserProfileProjection> findAllSummaries();

	// Per admin
	@Query("SELECT u.id as id, u.name as name, u.surname as surname, u.email as email, u.role as role, u.creationDate as creationDate, u.lastLogin as lastLogin FROM User u")
	List<UserProfileProjection> findAllForAdmin();

	@Query("SELECT u FROM User u WHERE u.role = :role AND u.status IN :statuses " +
			"AND (LOWER(u.name) LIKE LOWER(CONCAT('%', :search, '%')) OR LOWER(u.surname) LIKE LOWER(CONCAT('%', :search, '%')))")
	Page<User> findCustomers(@Param("role") UserRole role, @Param("statuses") List<UserStatus> statuses,
			@Param("search") String search, Pageable pageable);

	@Query("SELECT u FROM User u WHERE u.role = :role AND u.status IN :statuses " +
			"AND LOWER(u.name) LIKE LOWER(CONCAT('%', :search, '%'))")
	Page<User> findProviders(@Param("role") UserRole role, @Param("statuses") List<UserStatus> statuses,
			@Param("search") String search, Pageable pageable);

	List<User> findByStatus(UserStatus status);

	long countByStatus(UserStatus status);

	long countByRoleAndStatus(UserRole role, UserStatus status);

	@Query("SELECT new us.hogu.model.internal.ProviderServicesCheck(" +
			"(SELECT count(b) > 0 FROM BnbServiceEntity b WHERE b.user.id = :userId), " +
			"(SELECT count(c) > 0 FROM ClubServiceEntity c WHERE c.user.id = :userId), " +
			"(SELECT count(r) > 0 FROM RestaurantServiceEntity r WHERE r.user.id = :userId), " +
			"(SELECT count(l) > 0 FROM LuggageServiceEntity l WHERE l.user.id = :userId), " +
			"(SELECT count(n) > 0 FROM NccServiceEntity n WHERE n.user.id = :userId) " +
			") FROM User u WHERE u.id = :userId")
	ProviderServicesCheck checkProviderServices(@Param("userId") Long userId);
}
