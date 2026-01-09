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
import us.hogu.controller.dto.request.RestaurantBookingRequestDto;
import us.hogu.controller.dto.response.RestaurantBookingResponseDto;
import us.hogu.service.intefaces.RestaurantService;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/services/restaurants")
@PreAuthorize("hasAnyRole(T(us.hogu.model.enums.UserRole).CUSTOMER.name())")
@Tag(name = "Restaurant Services Customer", description = "APIs per gestione ristoranti e prenotazioni per il profilo del cliente")
public class ResturantCustomerController {
    private final RestaurantService restaurantService;
    

    @GetMapping("/bookings/my-bookings")
    @Operation(summary = "Le mie prenotazioni ristoranti (paginato)", description = "Restituisce in modo paginato le prenotazioni ristoranti dell'utente autenticato")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Prenotazioni recuperate con successo"),
        @ApiResponse(responseCode = "401", description = "Utente non autenticato")
    })
    public ResponseEntity<Page<RestaurantBookingResponseDto>> getUserRestaurantBookings(
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

        Page<RestaurantBookingResponseDto> response = restaurantService.getUserRestaurantBookings(userAccount.getAccountId(), pageable);
        return ResponseEntity.ok(response);
    }
    
    @PostMapping("/{id}/bookings")
    @Operation(summary = "Crea prenotazione ristorante", description = "Crea una nuova prenotazione per il ristorante specificato")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Prenotazione creata con successo"),
        @ApiResponse(responseCode = "400", description = "Dati prenotazione non validi"),
        @ApiResponse(responseCode = "404", description = "Ristorante non trovato"),
        @ApiResponse(responseCode = "409", description = "Non c'è disponibilità per l'orario selezionato")
    })
    public ResponseEntity<RestaurantBookingResponseDto> createRestaurantBooking(
    		@Parameter(hidden = true)
	        @AuthenticationPrincipal UserAccount userAccount,
            @Parameter(description = "ID del ristorante") @PathVariable Long id,
            @Valid @RequestBody RestaurantBookingRequestDto requestDto) 
    {
        RestaurantBookingResponseDto response = restaurantService.createRestaurantBooking(requestDto, userAccount.getAccountId());
        
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
    
}
