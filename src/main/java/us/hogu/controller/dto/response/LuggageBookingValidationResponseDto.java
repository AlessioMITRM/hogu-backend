package us.hogu.controller.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class LuggageBookingValidationResponseDto {
    private boolean valid;
    private String serviceType;
    private Long bookingId;
    private String firstName;
    private String lastName;
    private String fullName;
    private String serviceName;
    private String dropOffTime;
    private String pickUpTime;
    private Integer bagsSmall;
    private Integer bagsMedium;
    private Integer bagsLarge;
    private String totalAmount;
}
