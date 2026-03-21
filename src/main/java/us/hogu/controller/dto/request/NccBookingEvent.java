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
public class NccBookingEvent implements Serializable {
    private Long userId;
    private Long nccServiceId;
    private Long bookingId;
    private OffsetDateTime pickupTime;
    private String pickupLocation;
    private String destination;
    private BigDecimal totalAmount;
    private String billingFirstName;
    private String billingLastName;
    private Double pickupLatitude;
    private Double pickupLongitude;
    private Double destinationLatitude;
    private Double destinationLongitude;
}
