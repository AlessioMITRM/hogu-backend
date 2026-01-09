package us.hogu.controller.provider;

import java.io.IOException;
import java.util.List;

import javax.validation.Valid;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import us.hogu.configuration.security.dto.UserAccount;
import us.hogu.controller.dto.request.RestaurantServiceRequestDto;
import us.hogu.controller.dto.response.RestaurantBookingResponseDto;
import us.hogu.controller.dto.response.RestaurantManagementResponseDto;
import us.hogu.controller.dto.response.ServiceDetailResponseDto;
import us.hogu.repository.projection.RestaurantManagementProjection;
import us.hogu.service.intefaces.RestaurantService;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/services/restaurants")
@PreAuthorize("hasAnyRole(T(us.hogu.model.enums.UserRole).PROVIDER.name())")
@Tag(name = "Restaurant Services Provider", description = "APIs per gestione ristoranti e prenotazioni per il profilo del fornitore")
public class ResturantProviderController {
    private final RestaurantService restaurantService;
    
    
    @GetMapping("/{id}/bookings")
    @Operation(summary = "Prenotazioni ristorante", description = "Restituisce le prenotazioni ricevute per un ristorante (solo proprietario)")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Prenotazioni recuperate con successo"),
        @ApiResponse(responseCode = "401", description = "Utente non autenticato o non autorizzato"),
        @ApiResponse(responseCode = "404", description = "Ristorante non trovato")
    })
    public ResponseEntity<Page<RestaurantBookingResponseDto>> getRestaurantBookings(
            @Parameter(hidden = true)
            @AuthenticationPrincipal UserAccount userAccount,
            @Parameter(description = "ID del ristorante") @PathVariable Long id,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "creationDate") String sortBy,
            @RequestParam(defaultValue = "desc") String direction
    ) {
        Pageable pageable = PageRequest.of(
            page, size,
            direction.equalsIgnoreCase("desc") ? Sort.by(sortBy).descending() : Sort.by(sortBy).ascending()
        );

        Page<RestaurantBookingResponseDto> response = restaurantService.getRestaurantBookings(id, userAccount.getAccountId(), pageable);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/provider/my-restaurants")
    @Operation(summary = "I miei ristoranti (paginati)", description = "Restituisce la lista dei ristoranti del fornitore autenticato con paginazione")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Ristoranti recuperati con successo"),
        @ApiResponse(responseCode = "401", description = "Utente non autenticato o non fornitore")
    })
    public ResponseEntity<Page<RestaurantManagementResponseDto>> getProviderRestaurants(
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

        Page<RestaurantManagementResponseDto> response = restaurantService.getProviderRestaurants(userAccount.getAccountId(), 
        		pageable);
        return ResponseEntity.ok(response);
    }
    
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Crea nuovo ristorante", description = "Crea un nuovo ristorante (solo fornitore)")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Ristorante creato con successo"),
        @ApiResponse(responseCode = "400", description = "Dati ristorante non validi"),
        @ApiResponse(responseCode = "401", description = "Utente non autenticato o non fornitore")
    })
    public ResponseEntity<ServiceDetailResponseDto> createRestaurant(
            @Parameter(hidden = true)
            @AuthenticationPrincipal UserAccount userAccount,
            @RequestPart("data") @Valid RestaurantServiceRequestDto request,
            @RequestPart(value = "images", required = false) List<MultipartFile> images)
    throws IOException {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(restaurantService.createRestaurant(userAccount.getAccountId(), request, images));
    }
    
    @PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Aggiorna ristorante", description = "Aggiorna ristorante")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Ristorante aggiornato con successo"),
        @ApiResponse(responseCode = "400", description = "Dati ristorante non validi"),
        @ApiResponse(responseCode = "401", description = "Utente non autenticato o non fornitore")
    })
    public ResponseEntity<ServiceDetailResponseDto> updateRestaurant(
            @Parameter(hidden = true)
            @AuthenticationPrincipal UserAccount userAccount,
            @Parameter(description = "ID del servizio RESTURANT") @PathVariable Long id,
            @RequestPart("data") @Valid RestaurantServiceRequestDto request,
            @RequestPart(value = "images", required = false) List<MultipartFile> images)
    throws IOException {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(restaurantService.updateRestaurant(userAccount.getAccountId(), id, request, images));
    }

}
