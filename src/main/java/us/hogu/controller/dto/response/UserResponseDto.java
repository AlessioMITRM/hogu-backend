package us.hogu.controller.dto.response;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;

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
    
    private String email;
    
    private UserRole role;
    
    private ServiceType serviceType;
    
    private OffsetDateTime creationDate;
    
    private OffsetDateTime lastLogin;
}
