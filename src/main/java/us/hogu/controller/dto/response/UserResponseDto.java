package us.hogu.controller.dto.response;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.List;

import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import us.hogu.model.enums.ServiceType;
import us.hogu.model.enums.UserRole;

@Builder
@Getter
public class UserResponseDto {
    private Long id;

    private String name;

    private String surname;

    private String fiscalCode;

    private String email;

    private UserRole role;

    private ServiceType serviceType;

    private String iban;

    private OffsetDateTime creationDate;

    private OffsetDateTime lastLogin;

    private List<ServiceLocaleResponseDto> serviceLocales;
}
