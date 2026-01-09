package us.hogu.controller.customer;

import javax.validation.Valid;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import us.hogu.configuration.security.dto.UserAccount;
import us.hogu.controller.dto.request.LuggageBookingRequestDto;
import us.hogu.controller.dto.response.LuggageBookingResponseDto;
import us.hogu.service.intefaces.ClubService;
import us.hogu.service.intefaces.LuggageService;

@RestController
@RequestMapping("/api/services/luggage/customer")
@RequiredArgsConstructor
@Tag(name = "Luggage Services Customer", description = "APIs per gestione Luggage del Customer")
@PreAuthorize("hasAnyRole(T(us.hogu.model.enums.UserRole).CUSTOMER.name())")
public class LuggageCustomerController {
    private final LuggageService luggageService;

    
    @GetMapping("/bookings/my-bookings")
    @Operation(summary = "Le mie prenotazioni bagagli (paginato)", description = "Restituisce la lista delle prenotazioni bagagli dell’utente autenticato con supporto alla paginazione")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Prenotazioni recuperate con successo"),
        @ApiResponse(responseCode = "401", description = "Utente non autenticato o non autorizzato")
    })
    public ResponseEntity<Page<LuggageBookingResponseDto>> getUserLuggageBookings(
            @Parameter(hidden = true)
            @AuthenticationPrincipal UserAccount userAccount,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "creationDate") String sortBy,
            @RequestParam(defaultValue = "desc") String direction
    ) {
        Pageable pageable = PageRequest.of(
            page, size,
            direction.equalsIgnoreCase("desc") ? Sort.by(sortBy).descending() : Sort.by(sortBy).ascending()
        );

        Page<LuggageBookingResponseDto> response = luggageService.getUserLuggageBookings(userAccount.getAccountId(), pageable);
        return ResponseEntity.ok(response);
    }

    
    @PostMapping("/bookings")
    @Operation(summary = "Crea prenotazione bagagli", description = "Crea una nuova prenotazione per un servizio bagagli")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Prenotazione creata con successo"),
        @ApiResponse(responseCode = "400", description = "Dati prenotazione non validi"),
        @ApiResponse(responseCode = "404", description = "Servizio bagagli non trovato"),
        @ApiResponse(responseCode = "409", description = "Non c'è disponibilità per l'orario selezionato")
    })
    public ResponseEntity<LuggageBookingResponseDto> createLuggageBooking(
    		@Parameter(hidden = true)
	        @AuthenticationPrincipal UserAccount userAccount,
            @Valid @RequestBody LuggageBookingRequestDto requestDto) 
    {
        LuggageBookingResponseDto response = luggageService.createLuggageBooking(userAccount.getAccountId(), requestDto);
        
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
    
}
