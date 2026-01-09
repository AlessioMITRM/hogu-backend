package us.hogu.controller.provider;

import javax.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import us.hogu.controller.dto.request.OtpVerificationRequestDto;
import us.hogu.controller.dto.request.PasswordResetConfirmDto;
import us.hogu.controller.dto.request.PasswordResetDashboard;
import us.hogu.controller.dto.request.PasswordResetRequestDto;
import us.hogu.controller.dto.request.ProviderRegistrationRequestDto;
import us.hogu.controller.dto.request.UserLoginRequestDto;
import us.hogu.configuration.security.dto.UserAccount;
import us.hogu.controller.dto.request.CustomerRegistrationRequestDto;
import us.hogu.controller.dto.request.OtpResendRequestDto;
import us.hogu.controller.dto.response.AuthResponseDto;
import us.hogu.model.enums.UserRole;
import us.hogu.service.intefaces.UserService;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/provider/auth")
@Tag(name = "Authentication", description = "APIs per modifiche di autenticazione Provider")
@PreAuthorize("hasAnyRole(T(us.hogu.model.enums.UserRole).PROVIDER.name())")
public class ProviderAuthController {
    private final UserService userService;


    @PostMapping("/password-reset-account")
    @Operation(summary = "Richiesta Reset Password", description = "Resettare la password dell'account")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Richiesta presa in carico (OTP inviato se email esiste)"),
        @ApiResponse(responseCode = "400", description = "Formato email non valido"),
        @ApiResponse(responseCode = "404", description = "Email non trovata (a seconda della policy di sicurezza)")
    })
    public ResponseEntity<Void> requestPasswordReset(
    		@Parameter(hidden = true)
            @AuthenticationPrincipal UserAccount userAccount,
            @Valid @RequestBody PasswordResetDashboard request) 
    {
        userService.passwordResetDashboard(userAccount, request);
        
        return ResponseEntity.ok().build();
    }

}
