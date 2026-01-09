package us.hogu.controller.dto.request;

import javax.validation.constraints.Email;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

import lombok.Data;
import us.hogu.model.enums.UserRole;

@Data
public class CustomerRegistrationRequestDto {
    @NotBlank 
    private String name;
    
    @NotBlank 
    private String surname;
    
    @Email 
    private String email;
    
    @Size(min=6) 
    private String password;   
}
