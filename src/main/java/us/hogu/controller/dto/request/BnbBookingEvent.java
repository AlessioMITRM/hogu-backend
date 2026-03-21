package us.hogu.controller.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BnbBookingEvent implements Serializable {
    private Long userId;
    private Long roomId;
    private LocalDate checkIn;
    private LocalDate checkOut;
    private Integer guests;
    private BigDecimal totalAmount;
    private Long serviceId;
}
