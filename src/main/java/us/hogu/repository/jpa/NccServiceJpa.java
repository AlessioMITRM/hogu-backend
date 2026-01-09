package us.hogu.repository.jpa;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import us.hogu.model.NccServiceEntity;
import us.hogu.model.User;
import us.hogu.repository.projection.NccDetailProjection;
import us.hogu.repository.projection.NccManagementProjection;
import us.hogu.repository.projection.NccSummaryProjection;

public interface NccServiceJpa extends JpaRepository<NccServiceEntity, Long> {
    
	Optional<NccServiceEntity> findByIdAndUserId(long serviceId, long providerId);
	
	List<NccServiceEntity> findByUser(User user);
	
    // FRONTEND - lista servizi NCC attivi e pubblici
	@Query("SELECT DISTINCT n FROM NccServiceEntity n " +
		   "WHERE n.publicationStatus = true")
	List<NccServiceEntity> findActiveSummaries();

       @Query("SELECT DISTINCT n FROM NccServiceEntity n " +
              "JOIN n.locales loc " +
              "WHERE n.publicationStatus = true " +
              "AND (:citySearch IS NULL OR LOWER(loc.city) LIKE LOWER(CONCAT('%', :citySearch, '%'))) " +
              "AND (:stateSearch IS NULL OR LOWER(loc.state) LIKE LOWER(CONCAT('%', :stateSearch, '%'))) " +
              "AND (:countrySearch IS NULL OR LOWER(loc.country) LIKE LOWER(CONCAT('%', :countrySearch, '%'))) " +
              "AND (:language IS NULL OR LOWER(loc.language) = LOWER(:language)) " +
              "AND (:passengers IS NULL OR EXISTS ( " +
              "     SELECT 1 FROM VehicleEntity v " +
              "     WHERE v.nccService = n AND v.numberOfSeats >= :passengers ))")
       Page<NccServiceEntity> findActiveBySearch(
              @Param("citySearch") String citySearch,
              @Param("stateSearch") String stateSearch,
              @Param("countrySearch") String countrySearch,
              @Param("passengers") Integer passengers,
              @Param("language") String language,
              Pageable pageable);

    // FRONTEND - dettaglio servizio NCC pubblico
    @Query("SELECT n " +
           "FROM NccServiceEntity n WHERE n.id = :id AND n.publicationStatus = true")
    Optional<NccServiceEntity> findDetailById(Long id);
    
    // FORNITORE - lista servizi NCC del fornitore (anche non pubblicati)
    @Query("SELECT n FROM NccServiceEntity n WHERE n.user.id = :providerId")
    Page<NccServiceEntity> findByProviderId(Long providerId, Pageable page);
    
    // FORNITORE - dettaglio servizio NCC per modifica
    @Query("SELECT n.id as id, n.name as name, n.description as description, n.vehiclesAvailable as vehiclesAvailable, " +
           "n.basePrice as basePrice, n.locales as locales, n.images as images, " +
           "n.publicationStatus as publicationStatus, n.creationDate as creationDate " +
           "FROM NccServiceEntity n WHERE n.id = :id AND n.user.id = :providerId")
    Optional<NccServiceEntity> findDetailByIdAndProvider(Long id, Long providerId);
    
    // FORNITORE - servizi NCC attivi del fornitore
    @Query("SELECT n FROM NccServiceEntity n " +
           "WHERE n.user.id = :providerId AND n.publicationStatus = true")
    List<NccServiceEntity> findActiveByProviderId(Long providerId);
    
    // ADMIN - tutti i servizi NCC per gestione
    @Query("SELECT n FROM NccServiceEntity n")
    List<NccServiceEntity> findAllForAdmin();
    
    // ADMIN - servizi NCC in attesa di approvazione
    @Query("SELECT n FROM NccServiceEntity n WHERE n.publicationStatus = false")
    List<NccServiceEntity> findPendingApproval();
    
    // Controlli esistenza e autorizzazioni
    boolean existsByIdAndUserId(Long id, Long providerId);
    boolean existsByIdAndPublicationStatusTrue(Long id);
    
    // Ricerca per range di prezzo
    @Query("SELECT n FROM NccServiceEntity n " +
           "WHERE n.publicationStatus = true AND n.basePrice BETWEEN :minPrice AND :maxPrice")
    List<NccServiceEntity> findActiveByPriceRange(Double minPrice, Double maxPrice);
    
    // Statistiche fornitore
    @Query("SELECT COUNT(n) FROM NccServiceEntity n WHERE n.user.id = :providerId AND n.publicationStatus = true")
    Long countActiveByProvider(Long providerId);
    
    @Query("SELECT COUNT(n) FROM NccServiceEntity n WHERE n.user.id = :providerId")
    Long countTotalByProvider(Long providerId);
    
    // Per verificare se un fornitore ha già un servizio NCC con lo stesso nome
    @Query("SELECT COUNT(n) FROM NccServiceEntity n WHERE n.user.id = :providerId AND LOWER(n.name) = LOWER(:name)")
    Long countByProviderIdAndName(Long providerId, String name);
    
    // Servizi NCC con veicoli disponibili
    @Query("SELECT n.id as id, n.name as name, n.description as description, " +
           "n.basePrice as basePrice, n.locales as locales, n.images as images " +
           "FROM NccServiceEntity n WHERE n.publicationStatus = true AND n.vehiclesAvailable IS NOT NULL")
    List<NccServiceEntity> findActiveWithVehicles();
    
    // Per homepage - servizi NCC popolari (con più prenotazioni)
    @Query("SELECT n FROM NccServiceEntity n LEFT JOIN NccBooking b ON b.nccService = n " +
            "WHERE n.publicationStatus = true GROUP BY n ORDER BY COUNT(b) DESC")
    List<NccServiceEntity> findPopularActiveServices();
}
