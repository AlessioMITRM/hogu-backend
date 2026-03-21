package us.hogu.repository.jpa;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import us.hogu.controller.dto.response.InfoStatsDto;
import us.hogu.model.BnbServiceEntity;
import us.hogu.model.User;

public interface BnbServiceJpa extends JpaRepository<BnbServiceEntity, Long> {

    Page<BnbServiceEntity> findByUserId(Long userId, Pageable pageable);

    List<BnbServiceEntity> findByUser(User user);

    Page<BnbServiceEntity> findByPublicationStatusTrue(Pageable pageable);

    List<BnbServiceEntity> findByPublicationStatus(Boolean publicationStatus);

    Optional<BnbServiceEntity> findByIdAndUserId(Long id, Long userId);

    @Query("SELECT b FROM BnbServiceEntity b WHERE b.user.id = :providerId")
    Optional<BnbServiceEntity> findByProviderIdForSingleService(@Param("providerId") Long providerId);

    @Query("SELECT new us.hogu.controller.dto.response.InfoStatsDto(" +
            "   b.id, " +
            "   b.name, " +
            "   b.description, " +
            "   (SELECT COUNT(bk) FROM BnbBooking bk WHERE bk.bnbService = b), " +
            "   (SELECT COALESCE(SUM(bk.totalAmount), 0) FROM BnbBooking bk WHERE bk.bnbService = b) " +
            ") " +
            "FROM BnbServiceEntity b WHERE b.user.id = :providerId")
    InfoStatsDto getInfoStatsByProviderId(@Param("providerId") Long providerId);
}
