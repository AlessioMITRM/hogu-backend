package us.hogu.controller.dto.request;

import javax.validation.constraints.Email;
import javax.validation.constraints.NotBlank;

import lombok.Data;

@Data
public class OtpVerificationRequestDto {
	
	@Email 
	public String email;
	
	@NotBlank
	public String otpCode;
}
