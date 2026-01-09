package us.hogu.controller;

import javax.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import us.hogu.configuration.security.dto.UserAccount;
import us.hogu.controller.dto.request.UserUpdateRequestDto;
import us.hogu.controller.dto.response.UserProfileResponseDto;
import us.hogu.controller.dto.response.UserResponseDto;
import us.hogu.service.intefaces.UserService;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Tag(name = "Users", description = "APIs per gestione profilo utente")
public class UserController {

    private final UserService userService;

    @GetMapping("/profile")
    @Operation(summary = "Ottieni profilo utente", description = "Restituisce i dati del profilo dell'utente autenticato")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Profilo recuperato con successo"),
        @ApiResponse(responseCode = "401", description = "Utente non autenticato"),
        @ApiResponse(responseCode = "404", description = "Utente non trovato")
    })
    public ResponseEntity<UserResponseDto> getUserProfile(	
    		@Parameter(hidden = true)
	        @AuthenticationPrincipal UserAccount userAccount) {
        UserResponseDto response = userService.getUserProfile(userAccount.getAccountId());
        return ResponseEntity.ok(response);
    }

    @PutMapping("/profile")
    @Operation(summary = "Aggiorna profilo utente", description = "Aggiorna i dati del profilo dell'utente autenticato")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Profilo aggiornato con successo"),
        @ApiResponse(responseCode = "400", description = "Dati di aggiornamento non validi"),
        @ApiResponse(responseCode = "401", description = "Utente non autenticato")
    })
    public ResponseEntity<UserProfileResponseDto> updateUserProfile(
    		@Parameter(hidden = true)
    		@AuthenticationPrincipal UserAccount userAccount,
            @Valid @RequestBody UserUpdateRequestDto requestDto) 
    {
        UserProfileResponseDto response = userService.updateUserProfile(userAccount.getAccountId(), requestDto);
        return ResponseEntity.ok(response);
    }

}
