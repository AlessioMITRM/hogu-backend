package us.hogu.controller.publicapi;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import us.hogu.controller.dto.response.ServiceDetailResponseDto;
import us.hogu.controller.dto.response.ServiceSummaryResponseDto;
import us.hogu.controller.dto.request.RestaurantAdvancedSearchRequestDto;
import us.hogu.controller.dto.request.RestaurantAvailabilityRequestDto;
import us.hogu.controller.dto.response.RestaurantAvailabilityResponseDto;
import us.hogu.service.intefaces.RestaurantService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@RestController
@RequestMapping("/api/public/services/restaurant")
@RequiredArgsConstructor
@Tag(name = "Restaurant Services Public", description = "API pubbliche per consultare servizi RESTAURANT senza autenticazione")
public class RestaurantPublicController {
    private final RestaurantService restaurantService;

    
    @GetMapping
    @Operation(summary = "Lista ristoranti attivi (paginata)",
               description = "Restituisce la lista di tutti i ristoranti pubblici e attivi con supporto a paginazione e ordinamento")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Lista ristoranti recuperata con successo")
    })
    public ResponseEntity<Page<ServiceSummaryResponseDto>> getActiveRestaurants(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id,desc") String sort) 
    {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Order.desc(sort.split(",")[0])));
        Page<ServiceSummaryResponseDto> response = restaurantService.getActiveRestaurants(pageable);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Dettaglio ristorante", description = "Restituisce i dettagli completi di un ristorante")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Dettaglio ristorante recuperato con successo"),
        @ApiResponse(responseCode = "404", description = "Ristorante non trovato")
    })
    public ResponseEntity<ServiceDetailResponseDto> getRestaurantDetail(
            @Parameter(description = "ID del ristorante") @PathVariable Long id,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(required = false) Integer numberOfPeople) 
    {
        ServiceDetailResponseDto response = restaurantService.getRestaurantDetail(id, date, numberOfPeople);    
        return ResponseEntity.ok(response);
    }

   /* @GetMapping("/search")
    @Operation(summary = "Cerca ristoranti", description = "Cerca ristoranti per nome o indirizzo")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Ricerca completata con successo")
    })
    public ResponseEntity<List<ServiceSummaryResponseDto>> searchRestaurants(
            @Parameter(description = "Termine di ricerca") @RequestParam String searchTerm) {
        List<ServiceSummaryResponseDto> response = restaurantService.searchRestaurants(searchTerm);
        
        return ResponseEntity.ok(response);
    }*/

    @PostMapping("/search")
    @Operation(summary = "Ricerca avanzata ristoranti", description = "Ricerca ristoranti con parametri avanzati")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Ricerca avanzata completata con successo")
    })
    public ResponseEntity<Page<ServiceSummaryResponseDto>> advancedSearchRestaurants(
            @RequestBody RestaurantAdvancedSearchRequestDto searchRequest,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) 
    {
        Pageable pageable = PageRequest.of(page, size);
        Page<ServiceSummaryResponseDto> response = restaurantService.advancedSearchRestaurants(searchRequest, pageable);
        
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/availability")
    @Operation(summary = "Verifica disponibilità ristorante", description = "Controlla la disponibilità di un ristorante per una specifica data e ora")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Disponibilità verificata con successo"),
        @ApiResponse(responseCode = "404", description = "Ristorante non trovato")
    })
    public ResponseEntity<RestaurantAvailabilityResponseDto> checkRestaurantAvailability(
            @Parameter(description = "ID del ristorante") @PathVariable Long id,
            @RequestBody RestaurantAvailabilityRequestDto availabilityRequest) {
        RestaurantAvailabilityResponseDto response = restaurantService.checkRestaurantAvailability(id, availabilityRequest);
        
        return ResponseEntity.ok(response);
    }
}
