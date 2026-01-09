package us.hogu.repository.jpa;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import feign.Param;
import us.hogu.model.RestaurantServiceEntity;
import us.hogu.model.User;
import us.hogu.repository.projection.RestaurantDetailProjection;
import us.hogu.repository.projection.RestaurantManagementProjection;
import us.hogu.repository.projection.RestaurantSummaryProjection;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface RestaurantServiceJpa extends JpaRepository<RestaurantServiceEntity, Long>, JpaSpecificationExecutor<RestaurantServiceEntity> {
   
    boolean existsByIdAndUserId(Long id, Long userId);

    // Frontend - lista pubblica
    @Query(
    	    value = "SELECT DISTINCT r FROM RestaurantServiceEntity r JOIN FETCH r.locales loc WHERE r.publicationStatus = true AND LOWER(loc.language) = LOWER(:language)",
    	    countQuery = "SELECT COUNT(DISTINCT r) FROM RestaurantServiceEntity r JOIN r.locales loc WHERE r.publicationStatus = true AND LOWER(loc.language) = LOWER(:language)"
    	)
    Page<RestaurantServiceEntity> findActiveByLanguage(@Param("language") String language, Pageable pageable);

    // Fornitore - gestione servizi
    @Query("SELECT r FROM RestaurantServiceEntity r WHERE r.user.id = :providerId")
    Page<RestaurantServiceEntity> findByProviderId(Long providerId, Pageable pageable);
    
    // Admin - tutti i servizi
    @Query("SELECT r FROM RestaurantServiceEntity r")
    List<RestaurantServiceEntity> findAllForAdmin();
    
    // DETTAGLIO COMPLETO per pagina servizio
    @Query("SELECT r FROM RestaurantServiceEntity r WHERE r.id = :id AND r.publicationStatus = true")
    Optional<RestaurantServiceEntity> findDetailById(Long id);
    
    // DETTAGLIO per fornitore (include anche non pubblicati)
    @Query("SELECT r FROM RestaurantServiceEntity r WHERE r.id = :id AND r.user.id = :providerId")
    Optional<RestaurantServiceEntity> findDetailByIdAndProvider(Long id, Long providerId);
    
    // Frontend - lista pubblica
    @Query("SELECT DISTINCT r FROM RestaurantServiceEntity r " +
    	       "JOIN r.locales loc " +
    	       "WHERE r.publicationStatus = true " +
    	       "AND (COALESCE(:searchText, '') = '' " +
    	       "     OR LOWER(r.name) LIKE LOWER(CONCAT('%', :searchText, '%')) " +
    	       "     OR LOWER(loc.city) LIKE LOWER(CONCAT('%', :searchText, '%')) " +
    	       "     OR LOWER(loc.state) LIKE LOWER(CONCAT('%', :searchText, '%')) " +
    	       "     OR LOWER(loc.country) LIKE LOWER(CONCAT('%', :searchText, '%'))) " +
    	       "AND (COALESCE(:language, '') = '' " +
    	       "     OR LOWER(loc.language) = LOWER(:language))")
    List<RestaurantServiceEntity> findActiveBySearchAndLanguage(
    	        @Param("searchText") String searchText,
    	        @Param("language") String language);

    List<RestaurantServiceEntity> findByUser(User user);
}
