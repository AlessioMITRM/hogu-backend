package us.hogu.controller.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ClubBookingValidationResponseDto {
    private boolean valid;
    private Long bookingId;
    private Long eventId;
    private String fullName;
    private String date;
    private String time;
    private Integer guests;
}
