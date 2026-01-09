package us.hogu.repository.projection;

import java.time.OffsetDateTime;

public interface PayoutProjection {
	Long getId();
	
    Double getAmount();
    
    OffsetDateTime getPaymentDate();
    
    String getStatus();
}
