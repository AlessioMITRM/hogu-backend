package us.hogu.repository.jpa;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import us.hogu.model.UserDocument;
import us.hogu.model.UserServiceVerification;
import us.hogu.repository.projection.UserDocumentForGetAllProjection;

public interface UserDocumentJpa extends JpaRepository<UserDocument, Long> {

	List<UserDocument> findByUserServiceVerification(UserServiceVerification userServiceVerification);

	@Query("SELECT u FROM UserDocument u WHERE u.userServiceVerification = :userServiceVerification")
	List<UserDocumentForGetAllProjection> findForGetAll(@Param("userServiceVerification") UserServiceVerification userServiceVerification);


}
