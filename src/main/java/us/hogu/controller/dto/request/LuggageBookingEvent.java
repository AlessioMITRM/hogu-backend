package us.hogu.controller.dto.request;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.OffsetDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LuggageBookingEvent implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long userId;
    private Long luggageServiceId;
    private Long bookingId;
    private OffsetDateTime dropOffTime;
    private OffsetDateTime pickUpTime;
    private Integer bagsSmall;
    private Integer bagsMedium;
    private Integer bagsLarge;
    private BigDecimal totalAmount;
    private String billingFirstName;
    private String billingLastName;
}
