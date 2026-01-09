package us.hogu.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import us.hogu.common.constants.CommissionConstants;
import us.hogu.model.AppliedCommission;
import us.hogu.model.CommissionSetting;
import us.hogu.model.enums.ServiceType;
import us.hogu.repository.jpa.CommissionSettingJpa;
import us.hogu.service.intefaces.CommissionService;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CommissionServiceImpl implements CommissionService {

    private final CommissionSettingJpa commissionSettingJpa;

    @Override
    @Transactional(readOnly = true)
    public Double calculateCommissionAmount(Double bookingAmount, ServiceType serviceType) {
        CommissionSetting commissionSetting = getCurrentCommissionSetting(serviceType);
        
        if (commissionSetting == null) {
            // Commissione di default se non trovata configurazione
            return calculateDefaultCommission(bookingAmount);
        }
        
        Double commissionAmount = bookingAmount * (commissionSetting.getCommissionRate() / 100);
        
        // Applica commissione minima se specificata
        if (commissionSetting.getMinCommissionAmount() != null && 
            commissionAmount < commissionSetting.getMinCommissionAmount()) {
            return commissionSetting.getMinCommissionAmount();
        }
        
        return commissionAmount;
    }

    @Override
    @Transactional(readOnly = true)
    public AppliedCommission calculateCommission(Object booking, ServiceType serviceType) {
        Double bookingAmount = getBookingAmount(booking, serviceType);
        CommissionSetting commissionSetting = getCurrentCommissionSetting(serviceType);
        
        Double commissionRate = commissionSetting != null ? 
            commissionSetting.getCommissionRate() : getDefaultCommissionRate();
        Double commissionAmount = calculateCommissionAmount(bookingAmount, serviceType);
        
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
        Double defaultRate = getDefaultCommissionRateForService(serviceType);
        Double minCommission = getDefaultMinCommissionForService(serviceType);
        
        return CommissionSetting.builder()
            .serviceType(serviceType)
            .commissionRate(defaultRate)
            .minCommissionAmount(minCommission)
            .effectiveFrom(OffsetDateTime.now())
            .build();
    }

    private Double getDefaultCommissionRate() {
        return CommissionConstants.COMMISSION;
    }

    private Double getDefaultCommissionRateForService(ServiceType serviceType) {
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

    private Double getDefaultMinCommissionForService(ServiceType serviceType) {
        switch (serviceType) {
            case RESTAURANT:
                return 1.0;  // Min 1€ per ristoranti
            case NCC:
                return 3.0;  // Min 3€ per NCC
            case CLUB:
                return 2.0;  // Min 2€ per club
            case LUGGAGE:
                return 0.5;  // Min 0.5€ per bagagli
            default:
                throw new IllegalArgumentException("Tipo di servizio non gestito: " + serviceType);
        }
    }

    private Double calculateDefaultCommission(Double bookingAmount) {
        return bookingAmount * (getDefaultCommissionRate() / 100);
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

    private Double getBookingAmount(Object booking, ServiceType serviceType) {
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