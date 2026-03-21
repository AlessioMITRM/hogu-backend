package us.hogu.controller.dto.response;

import java.time.OffsetDateTime;
import java.util.List;

import lombok.Builder;
import lombok.Getter;
import us.hogu.model.enums.UserRole;

@Builder
@Getter
public class UserProfileResponseDto {
	private Long id;

	private String name;

	private String surname;

	private String fiscalCode;

	private String email;

	private UserRole role;

	private String iban;

	private OffsetDateTime lastLogin;

	private OffsetDateTime creationDate;

	private List<ServiceLocaleResponseDto> serviceLocales;
}
