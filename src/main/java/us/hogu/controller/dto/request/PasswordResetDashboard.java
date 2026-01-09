package us.hogu.controller.dto.request;

import javax.validation.constraints.Size;

import lombok.Data;

@Data
public class PasswordResetDashboard {
	@Size(min=6) 
	private String password;   
}
