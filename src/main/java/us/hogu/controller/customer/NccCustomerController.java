package us.hogu.controller.customer;

import java.util.List;

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
import org.springframework.web.bind.annotation.PathVariable;
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
import us.hogu.controller.dto.request.NccBookingRequestDto;
import us.hogu.controller.dto.request.NccServiceRequestDto;
import us.hogu.controller.dto.response.NccBookingResponseDto;
import us.hogu.controller.dto.response.ServiceDetailResponseDto;
import us.hogu.controller.dto.response.ServiceSummaryResponseDto;
import us.hogu.repository.projection.NccManagementProjection;
import us.hogu.service.intefaces.NccService;

@RestController
@RequestMapping("/api/services/ncc/customer")
@RequiredArgsConstructor
@Tag(name = "NCC Services Customer", description = "APIs per gestione servizi NCC e prenotazioni per utente Customer")
@PreAuthorize("hasAnyRole(T(us.hogu.model.enums.UserRole).CUSTOMER.name())")
public class NccCustomerController {
    private final NccService nccService;

    
    @GetMapping("/bookings/my-bookings")
    @Operation(summary = "Le mie prenotazioni NCC (paginato)", description = "Restituisce in modo paginato le prenotazioni NCC dell'utente autenticato")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Prenotazioni recuperate con successo"),
        @ApiResponse(responseCode = "401", description = "Utente non autenticato")
    })
    public ResponseEntity<Page<NccBookingResponseDto>> getUserNccBookings(
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

        Page<NccBookingResponseDto> response = nccService.getUserNccBookings(userAccount.getAccountId(), pageable);
        return ResponseEntity.ok(response);
    }
    
    @PostMapping("/{id}/bookings")
    @Operation(summary = "Crea prenotazione NCC", description = "Crea una nuova prenotazione per il servizio NCC specificato")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Prenotazione creata con successo"),
        @ApiResponse(responseCode = "400", description = "Dati prenotazione non validi"),
        @ApiResponse(responseCode = "404", description = "Servizio NCC non trovato"),
        @ApiResponse(responseCode = "409", description = "Non c'è disponibilità per l'orario selezionato")
    })
    public ResponseEntity<NccBookingResponseDto> createNccBooking(
    		@Parameter(hidden = true)
	        @AuthenticationPrincipal UserAccount userAccount,
            @Parameter(description = "ID del servizio NCC") @PathVariable Long id,
            @Valid @RequestBody NccBookingRequestDto requestDto) 
    {        
        NccBookingResponseDto response = nccService.createNccBooking(requestDto, userAccount.getAccountId());
        
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
    
}
