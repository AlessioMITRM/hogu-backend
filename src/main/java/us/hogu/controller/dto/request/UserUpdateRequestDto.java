package us.hogu.controller.dto.request;

import javax.validation.constraints.Email;
import javax.validation.constraints.NotBlank;

import lombok.Data;

@Data
public class UserUpdateRequestDto {
	@NotBlank
    private String name;
	
	@NotBlank
    private String surname;
	
	@Email
    private String email;
}