package us.hogu.converter;

import java.time.OffsetDateTime;

import org.springframework.stereotype.Component;

import us.hogu.controller.dto.request.CustomerRegistrationRequestDto;
import us.hogu.controller.dto.request.ProviderRegistrationRequestDto;
import us.hogu.controller.dto.request.UserUpdateRequestDto;
import us.hogu.controller.dto.response.UserProfileResponseDto;
import us.hogu.controller.dto.response.UserResponseDto;
import us.hogu.model.User;

@Component
public class UserMapper {
    
    public User toCustomerEntity(CustomerRegistrationRequestDto dto) {
        return User.builder()
            .name(dto.getName())
            .surname(dto.getSurname())
            .email(dto.getEmail())
            .creationDate(OffsetDateTime.now())
            .build();
    }
    
    public User toProviderEntity(ProviderRegistrationRequestDto dto) {
        return User.builder()
            .name(dto.getName())
            .email(dto.getEmail())
            .creationDate(OffsetDateTime.now())
            .build();
    }
    
    public UserResponseDto toResponseDto(User user) {
        return UserResponseDto.builder()
            .id(user.getId())
            .name(user.getName())
            .surname(user.getSurname())
            .email(user.getEmail())
            .role(user.getRole())
            .creationDate(user.getCreationDate())
            .lastLogin(user.getLastLogin())
            .build();
    }
    
    public UserProfileResponseDto toProfileDto(User user) {
        return UserProfileResponseDto.builder()
            .id(user.getId())
            .name(user.getName())
            .surname(user.getSurname())
            .email(user.getEmail())
            .role(user.getRole())
            .creationDate(user.getCreationDate())
            .lastLogin(user.getLastLogin())
            .build();
    }
    
    public void updateEntityFromDto(UserUpdateRequestDto dto, User user) {
        if (dto.getName() != null) user.setName(dto.getName());
        if (dto.getSurname() != null) user.setSurname(dto.getSurname());
        if (dto.getEmail() != null) user.setEmail(dto.getEmail());
    }
}