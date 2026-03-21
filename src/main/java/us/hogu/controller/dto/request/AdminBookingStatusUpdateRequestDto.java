package us.hogu.controller.dto.request;

import javax.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import us.hogu.model.enums.BookingStatus;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AdminBookingStatusUpdateRequestDto {
    @NotNull(message = "Lo stato è obbligatorio")
    private BookingStatus status;

    @NotNull(message = "La motivazione è obbligatoria")
    private String statusReason;
}
