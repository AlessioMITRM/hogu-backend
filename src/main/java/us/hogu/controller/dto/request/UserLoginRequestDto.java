package us.hogu.controller.dto.request;

import javax.validation.constraints.Email;
import javax.validation.constraints.NotBlank;

import lombok.Data;

@Data
public class UserLoginRequestDto {
	@Email 
	private String email;
	 
	@NotBlank
	private String password;
	
	@NotBlank
	private String role;
}
