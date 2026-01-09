package us.hogu.repository.jpa;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import us.hogu.model.CommissionSetting;
import us.hogu.model.enums.ServiceType;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface CommissionSettingJpa extends JpaRepository<CommissionSetting, Long> {

    @Query("SELECT cs FROM CommissionSetting cs WHERE " +
           "cs.serviceType = :serviceType AND " +
           "cs.effectiveFrom <= :currentDate AND " +
           "(cs.effectiveTo IS NULL OR cs.effectiveTo >= :currentDate) " +
           "ORDER BY cs.effectiveFrom DESC")
    Optional<CommissionSetting> findActiveByServiceType(
            @Param("serviceType") ServiceType serviceType,
            @Param("currentDate") OffsetDateTime currentDate);

    @Query("SELECT cs FROM CommissionSetting cs WHERE " +
           "cs.serviceType = :serviceType " +
           "ORDER BY cs.effectiveFrom DESC")
    List<CommissionSetting> findByServiceTypeOrderByEffectiveFromDesc(
            @Param("serviceType") ServiceType serviceType);

    @Query("SELECT cs FROM CommissionSetting cs WHERE " +
           "cs.effectiveFrom <= :currentDate AND " +
           "(cs.effectiveTo IS NULL OR cs.effectiveTo >= :currentDate)")
    List<CommissionSetting> findAllActiveSettings(@Param("currentDate") LocalDateTime currentDate);
}