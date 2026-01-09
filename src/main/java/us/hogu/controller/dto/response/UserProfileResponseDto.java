package us.hogu.controller.dto.response;

import java.time.OffsetDateTime;

import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import us.hogu.model.enums.UserRole;

@Builder
@Getter
public class UserProfileResponseDto {
	private Long id;
	
	private String name;
	
	private String surname;
	
	private String email;
	
	private UserRole role;
	
    private OffsetDateTime lastLogin; 
	
	private OffsetDateTime creationDate;
}
