package us.hogu.controller.publicapi;

import javax.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import us.hogu.controller.dto.request.OtpVerificationRequestDto;
import us.hogu.controller.dto.request.PasswordResetConfirmDto;
import us.hogu.controller.dto.request.PasswordResetRequestDto;
import us.hogu.controller.dto.request.ProviderRegistrationRequestDto;
import us.hogu.controller.dto.request.UserLoginRequestDto;
import us.hogu.controller.dto.request.CustomerRegistrationRequestDto;
import us.hogu.controller.dto.request.OtpResendRequestDto;
import us.hogu.controller.dto.response.AuthResponseDto;
import us.hogu.model.enums.UserRole;
import us.hogu.service.intefaces.UserService;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/public/auth")
@Tag(name = "Authentication", description = "APIs per autenticazione e registrazione")
public class AuthController {
    private final UserService userService;

    
    @PostMapping("/customer-register")
    @Operation(summary = "Registrazione nuovo utente", description = "Crea un nuovo account cliente")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Utente registrato con successo"),
        @ApiResponse(responseCode = "400", description = "Dati di registrazione non validi"),
        @ApiResponse(responseCode = "409", description = "Email già registrata")
    })
    public ResponseEntity<AuthResponseDto> customerRegister(
            @Valid @RequestBody CustomerRegistrationRequestDto request) 
    {
        AuthResponseDto response = userService.customerRegistration(request);
        
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
    
    @PostMapping("/customer-otp-verification")
    @Operation(summary = "Verifica Otp Cliente", description = "Verifica dell'otp creato nella registrazione")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Verifica effettuata con successo"),
        @ApiResponse(responseCode = "401", description = "Credenziali non valide"),
        @ApiResponse(responseCode = "404", description = "Utente o Otp non trovato")
    })
    public ResponseEntity<AuthResponseDto> customerOtpVerification(
            @Valid @RequestBody OtpVerificationRequestDto request) 
    {
        AuthResponseDto response = userService.verificateOtpCustomer(request);
        
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
    
    @PostMapping("/provider-register")
    @Operation(summary = "Registrazione nuovo utente", description = "Crea un nuovo account cliente")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Utente registrato con successo"),
            @ApiResponse(responseCode = "400", description = "Dati di registrazione non validi"),
            @ApiResponse(responseCode = "409", description = "Email già registrata")
    })
    public ResponseEntity<AuthResponseDto> providerRegister(
            @Valid @ModelAttribute ProviderRegistrationRequestDto request) throws Exception {

        AuthResponseDto response = userService.providerRegistration(request);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
    
    @PostMapping("/provider-otp-verification")
    @Operation(summary = "Verifica Provider Cliente", description = "Verifica dell'otp creato nella registrazione")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Verifica effettuata con successo"),
        @ApiResponse(responseCode = "401", description = "Credenziali non valide"),
        @ApiResponse(responseCode = "404", description = "Utente o Otp non trovato")
    })
    public ResponseEntity<AuthResponseDto> providerOtpVerification(
            @Valid @RequestBody OtpVerificationRequestDto request) 
    {
        AuthResponseDto response = userService.verificateOtpProvider(request);
        
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
    
    @PostMapping("/resend-otp-verification")
    @Operation(summary = "Verifica Provider Cliente", description = "Verifica dell'otp creato nella registrazione")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Verifica effettuata con successo"),
        @ApiResponse(responseCode = "401", description = "Credenziali non valide"),
        @ApiResponse(responseCode = "404", description = "Utente o Otp non trovato")
    })
    public ResponseEntity<?> providerResendOtpVerification(
            @Valid @RequestBody OtpResendRequestDto request) 
    {
        userService.resendOtpVerification(request.getEmail());
        
        return ResponseEntity.ok().build();
    }
    
    @PostMapping("/login")
    @Operation(summary = "Login utente", description = "Autenticazione utente con email e password")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Login effettuato con successo"),
        @ApiResponse(responseCode = "401", description = "Credenziali non valide"),
        @ApiResponse(responseCode = "404", description = "Utente non trovato")
    })
    public ResponseEntity<AuthResponseDto> login(
            @Valid @RequestBody UserLoginRequestDto request) 
    {
        AuthResponseDto response = userService.login(request);
        
        return ResponseEntity.ok(response);
    }

    @PostMapping("/password-reset-request")
    @Operation(summary = "Richiesta Reset Password", description = "Invia un'email con un codice OTP per resettare la password")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Richiesta presa in carico (OTP inviato se email esiste)"),
        @ApiResponse(responseCode = "400", description = "Formato email non valido"),
        @ApiResponse(responseCode = "404", description = "Email non trovata (a seconda della policy di sicurezza)")
    })
    public ResponseEntity<Void> requestPasswordReset(
            @Valid @RequestBody PasswordResetRequestDto request) 
    {
        userService.requestPasswordReset(request);
        
        // Restituiamo 200 OK senza body
        return ResponseEntity.ok().build();
    }

    @PostMapping("/password-reset-confirm")
    @Operation(summary = "Conferma Reset Password", description = "Verifica l'OTP ricevuto e imposta la nuova password")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Password aggiornata con successo"),
        @ApiResponse(responseCode = "400", description = "OTP non valido, scaduto o password non conforme"),
        @ApiResponse(responseCode = "404", description = "Utente o OTP non trovato")
    })
    public ResponseEntity<Void> confirmPasswordReset(
            @Valid @RequestBody PasswordResetConfirmDto request) 
    {
        userService.confirmPasswordReset(request);
        
        // Restituiamo 200 OK senza body
        return ResponseEntity.ok().build();
    }
}
