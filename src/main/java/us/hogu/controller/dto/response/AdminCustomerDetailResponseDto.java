package us.hogu.controller.dto.response;

import java.time.OffsetDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import us.hogu.model.enums.UserStatus;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdminCustomerDetailResponseDto {
    private Long id;
    private String name;
    private String surname;
    private String email;
    private String fiscalCode;
    private UserStatus status;
    private OffsetDateTime creationDate;
    private OffsetDateTime lastLogin;
    private String language;
    private String state;
    private long totalBookings;
}
