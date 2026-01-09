package us.hogu.service.intefaces;

import us.hogu.model.AppliedCommission;
import us.hogu.model.CommissionSetting;
import us.hogu.model.enums.ServiceType;

public interface CommissionService {
    
    Double calculateCommissionAmount(Double bookingAmount, ServiceType serviceType);
    
    AppliedCommission calculateCommission(Object booking, ServiceType serviceType);
    
    CommissionSetting getCurrentCommissionSetting(ServiceType serviceType);
    
    CommissionSetting createCommissionSetting(CommissionSetting commissionSetting);
    
    CommissionSetting updateCommissionSetting(Long id, CommissionSetting commissionSetting);
    
    void disableCommissionSetting(Long id);
}
