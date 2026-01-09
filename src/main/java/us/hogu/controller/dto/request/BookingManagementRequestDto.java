package us.hogu.controller.dto.request;

import javax.validation.constraints.NotBlank;

import io.micrometer.core.lang.NonNull;
import lombok.Data;
import us.hogu.model.enums.BookingAction;

@Data
public class BookingManagementRequestDto {
    @NonNull
	private Long bookingId;
    
    @NotBlank
    private BookingAction action;
    
    @NotBlank
    private String reason;
}
