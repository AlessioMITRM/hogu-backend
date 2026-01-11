package us.hogu.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import us.hogu.common.constants.CommissionConstants;
import us.hogu.model.AppliedCommission;
import us.hogu.model.CommissionSetting;
import us.hogu.model.enums.ServiceType;
import us.hogu.repository.jpa.CommissionSettingJpa;
import us.hogu.service.intefaces.CommissionService;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class CommissionServiceImpl implements CommissionService {

    private final CommissionSettingJpa commissionSettingJpa;

    @Override
    @Transactional(readOnly = true)
    public BigDecimal calculateCommissionAmount(BigDecimal bookingAmount, ServiceType serviceType) {
        // Validazione input di base
        if (bookingAmount == null || bookingAmount.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }

        CommissionSetting commissionSetting = getCurrentCommissionSetting(serviceType);

        // Caso: nessuna configurazione specifica → usiamo la commissione di default
        if (commissionSetting == null) {
            return calculateDefaultCommission(bookingAmount);
        }

        // Recuperiamo il tasso percentuale (es: 15.5, 12.00, etc...)
        BigDecimal ratePercent = commissionSetting.getCommissionRate();

        // Controllo di sicurezza sul tasso (opzionale ma fortemente consigliato)
        if (ratePercent == null || ratePercent.compareTo(BigDecimal.ZERO) < 0 || 
            ratePercent.compareTo(new BigDecimal("100")) > 0) {
            
        	log.warn("Tasso commissione non valido per {}: {}, uso default", serviceType, ratePercent);
            return calculateDefaultCommission(bookingAmount);
        }

        // Calcolo: amount * (rate / 100)
        BigDecimal commissionRateFraction = ratePercent.divide(
            BigDecimal.valueOf(100), 
            6,                      // più decimali intermedi per maggiore precisione
            RoundingMode.HALF_UP
        );

        BigDecimal commissionAmount = bookingAmount
            .multiply(commissionRateFraction)
            .setScale(2, RoundingMode.HALF_UP);  // arrotondamento finale tipico per €

        // Applica eventuale commissione minima
        BigDecimal minCommission = commissionSetting.getMinCommissionAmount();
        if (minCommission != null && minCommission.compareTo(BigDecimal.ZERO) > 0) {
            if (commissionAmount.compareTo(minCommission) < 0) {
                return minCommission;
            }
        }

        return commissionAmount;
    }

    @Override
    @Transactional(readOnly = true)
    public AppliedCommission calculateCommission(Object booking, ServiceType serviceType) {
    	BigDecimal bookingAmount = getBookingAmount(booking, serviceType);
        CommissionSetting commissionSetting = getCurrentCommissionSetting(serviceType);
        
        BigDecimal commissionRate = commissionSetting != null ? 
            commissionSetting.getCommissionRate() : getDefaultCommissionRate();
        BigDecimal commissionAmount = calculateCommissionAmount(bookingAmount, serviceType);
        
        return AppliedCommission.builder()
            .id(getBookingId(booking, serviceType))
            .commissionAmount(commissionAmount)
            .commissionRateApplied(commissionRate)
            .calculatedAt(OffsetDateTime.now())
            .commissionSetting(commissionSetting != null ? commissionSetting : null)
            .build();
    }

    @Override
    @Transactional(readOnly = true)
    public CommissionSetting getCurrentCommissionSetting(ServiceType serviceType) {
        OffsetDateTime now = OffsetDateTime.now();
        
        return commissionSettingJpa.findActiveByServiceType(serviceType, now)
            .orElseGet(() -> getDefaultCommissionSetting(serviceType));
    }

    @Override
    @Transactional
    public CommissionSetting createCommissionSetting(CommissionSetting commissionSetting) {
        // Disabilita eventuali settings precedenti per lo stesso service type
        disablePreviousSettings(commissionSetting.getServiceType());
        
        return commissionSettingJpa.save(commissionSetting);
    }

    @Override
    @Transactional
    public CommissionSetting updateCommissionSetting(Long id, CommissionSetting updatedSetting) {
        return commissionSettingJpa.findById(id)
            .map(existingSetting -> {
                // Se cambia il service type, disabilita i vecchi settings
                if (!existingSetting.getServiceType().equals(updatedSetting.getServiceType())) {
                    disablePreviousSettings(updatedSetting.getServiceType());
                }
                
                existingSetting.setServiceType(updatedSetting.getServiceType());
                existingSetting.setCommissionRate(updatedSetting.getCommissionRate());
                existingSetting.setMinCommissionAmount(updatedSetting.getMinCommissionAmount());
                existingSetting.setEffectiveFrom(updatedSetting.getEffectiveFrom());
                existingSetting.setEffectiveTo(updatedSetting.getEffectiveTo());
                existingSetting.setLastUpdateDate(OffsetDateTime.now());
                
                return commissionSettingJpa.save(existingSetting);
            })
            .orElseThrow(() -> new IllegalArgumentException("Commission setting non trovato"));
    }

    @Override
    @Transactional
    public void disableCommissionSetting(Long id) {
        commissionSettingJpa.findById(id).ifPresent(setting -> {
            setting.setEffectiveTo(OffsetDateTime.now());
            setting.setLastUpdateDate(OffsetDateTime.now());
            commissionSettingJpa.save(setting);
        });
    }

    
    // METODI PRIVATI
    private CommissionSetting getDefaultCommissionSetting(ServiceType serviceType) {
        // Commissioni di default per ogni tipo di servizio
    	BigDecimal defaultRate = getDefaultCommissionRateForService(serviceType);
    	BigDecimal minCommission = getDefaultMinCommissionForService(serviceType);
        
        return CommissionSetting.builder()
            .serviceType(serviceType)
            .commissionRate(defaultRate)
            .minCommissionAmount(minCommission)
            .effectiveFrom(OffsetDateTime.now())
            .build();
    }

    private BigDecimal getDefaultCommissionRate() {
        return CommissionConstants.COMMISSION;
    }

    private BigDecimal getDefaultCommissionRateForService(ServiceType serviceType) {
        switch (serviceType) {
            case RESTAURANT:
                return CommissionConstants.COMMISSION;  
            case NCC:
                return CommissionConstants.COMMISSION; 
            case CLUB:
                return CommissionConstants.COMMISSION; 
            case LUGGAGE:
                return CommissionConstants.COMMISSION; 
            case BNB:
                return CommissionConstants.COMMISSION;
            default:
                throw new IllegalArgumentException("Tipo di servizio non gestito: " + serviceType);
        }
    }

    private BigDecimal getDefaultMinCommissionForService(ServiceType serviceType) {
        switch (serviceType) {
            case RESTAURANT:
                return new BigDecimal("1.0");  // Min 1€ per ristoranti
            case NCC:
                return new BigDecimal("3.0");  // Min 3€ per NCC
            case CLUB:
                return new BigDecimal("2.0");  // Min 2€ per club
            case LUGGAGE:
                return new BigDecimal("0.50");  // Min 0.5€ per bagagli
            default:
                throw new IllegalArgumentException("Tipo di servizio non gestito: " + serviceType);
        }
    }

    private BigDecimal calculateDefaultCommission(BigDecimal bookingAmount) {
        if (bookingAmount == null || bookingAmount.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }

        BigDecimal rate = getDefaultCommissionRate();           // es: 12.5
        BigDecimal percentage = rate.divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP);

        return bookingAmount
                .multiply(percentage)
                .setScale(2, RoundingMode.HALF_UP);               // arrotondamento finale a 2 decimali
    }

    private void disablePreviousSettings(ServiceType serviceType) {
        OffsetDateTime now = OffsetDateTime.now();
        commissionSettingJpa.findActiveByServiceType(serviceType, now)
            .ifPresent(setting -> {
                setting.setEffectiveTo(now);
                setting.setLastUpdateDate(now);
                commissionSettingJpa.save(setting);
            });
    }

    private BigDecimal getBookingAmount(Object booking, ServiceType serviceType) {
        switch (serviceType) {
            case RESTAURANT:
                return ((us.hogu.model.RestaurantBooking) booking).getTotalAmount();
            case NCC:
                return ((us.hogu.model.NccBooking) booking).getTotalAmount();
            case CLUB:
                return ((us.hogu.model.ClubBooking) booking).getTotalAmount();
            case LUGGAGE:
                return ((us.hogu.model.LuggageBooking) booking).getTotalAmount();
            default:
                throw new IllegalArgumentException("Tipo di servizio non gestito: " + serviceType);
        }
    }

    private Long getBookingId(Object booking, ServiceType serviceType) {
        switch (serviceType) {
            case RESTAURANT:
                return ((us.hogu.model.RestaurantBooking) booking).getId();
            case NCC:
                return ((us.hogu.model.NccBooking) booking).getId();
            case CLUB:
                return ((us.hogu.model.ClubBooking) booking).getId();
            case LUGGAGE:
                return ((us.hogu.model.LuggageBooking) booking).getId();
            default:
                throw new IllegalArgumentException("Tipo di servizio non gestito: " + serviceType);
        }
    }

}