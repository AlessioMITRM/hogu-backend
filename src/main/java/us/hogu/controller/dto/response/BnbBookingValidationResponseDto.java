package us.hogu.controller.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BnbBookingValidationResponseDto {

    private boolean valid;

    private Long bookingId;
    private Long bnbServiceId;
    private String roomName;

    private String fullName;
    private String checkInDate;
    private String checkOutDate;
    private Integer guests;
}
