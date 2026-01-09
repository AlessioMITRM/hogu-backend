package us.hogu.controller.dto.request;

import lombok.Data;

@Data
public class PasswordResetConfirmDto {
    private String email;
    private String otpCode;
    private String newPassword;
}
