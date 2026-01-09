package us.hogu.controller.dto.response;

import java.time.OffsetDateTime;

import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import us.hogu.model.internal.ProviderServicesCheck;

@Builder
@Getter
public class AuthResponseDto {
	private String token;
	
	private UserResponseDto user;
	
	private ProviderServicesCheck services;
	
	private OffsetDateTime expiresAt;
}
