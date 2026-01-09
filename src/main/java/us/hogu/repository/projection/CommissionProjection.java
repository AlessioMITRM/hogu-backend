package us.hogu.repository.projection;

import java.time.OffsetDateTime;

import us.hogu.model.enums.ServiceType;

public interface CommissionProjection {
    Long getId();
    
    ServiceType getServiceType();
    
    Double getCommissionRate();
    
    Double getMinCommissionAmount();
    
    OffsetDateTime getEffectiveFrom();
}
