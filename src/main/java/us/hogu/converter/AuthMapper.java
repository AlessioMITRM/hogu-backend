package us.hogu.converter;

import java.time.OffsetDateTime;

import org.springframework.stereotype.Component;

import us.hogu.controller.dto.response.AuthResponseDto;
import us.hogu.controller.dto.response.UserResponseDto;

@Component
public class AuthMapper {
    
    public AuthResponseDto toAuthResponseDto(String token, UserResponseDto user) {
        return AuthResponseDto.builder()
            .token(token)
            .user(user)
            .expiresAt(OffsetDateTime.now().plusHours(1))
            .build();
    }
}
