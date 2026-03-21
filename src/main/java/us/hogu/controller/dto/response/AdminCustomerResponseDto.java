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
public class AdminCustomerResponseDto {
    private Long id;
    private String name;
    private String surname;
    private String email;
    private UserStatus status;
    private OffsetDateTime creationDate;
}
