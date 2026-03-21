package us.hogu.controller.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class RestaurantBookingValidationResponseDto {
    private boolean valid;
    private Long bookingId;
    private String firstName;
    private String lastName;
    private String fullName;
    private String date;
    private String time;
    private Integer guests;
}

