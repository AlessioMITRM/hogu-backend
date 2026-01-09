package us.hogu.controller.dto.request;

import javax.validation.constraints.Size;

import lombok.Data;

@Data
public class PasswordResetRequestDto {
    private String email;
}
