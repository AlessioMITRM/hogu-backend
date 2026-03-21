package us.hogu.controller.dto.request;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.OffsetDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClubBookingEvent implements Serializable {
    private Long userId;
    
    private Long clubServiceId;
    
    private Long eventId;
    
    private OffsetDateTime reservationTime;
    
    private Integer numberOfPeople;
    
    private BigDecimal totalAmount;
    
    private String specialRequests;
    
    private String billingFirstName;
    
    private String billingLastName;

    private String fiscalCode;

    private String taxId;

    private us.hogu.controller.dto.response.EventPricingConfigurationResponseDto pricingConfiguration;
}
