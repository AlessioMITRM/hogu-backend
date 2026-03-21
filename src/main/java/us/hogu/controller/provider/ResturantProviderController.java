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
import org.springframework.web.bind.annotation.DeleteMapping;
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
import us.hogu.controller.dto.response.InfoStatsDto;
import us.hogu.controller.dto.response.RestaurantServiceDetailResponseDto;
import us.hogu.controller.dto.response.ServiceDetailResponseDto;
import us.hogu.controller.dto.response.RestaurantBookingValidationResponseDto;
import us.hogu.repository.projection.RestaurantManagementProjection;
import us.hogu.service.intefaces.RestaurantService;
import us.hogu.client.feign.dto.response.PaymentResponseDto;
import org.springframework.beans.factory.annotation.Value;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/provider/services/restaurant")
@PreAuthorize("hasAnyRole(T(us.hogu.model.enums.UserRole).PROVIDER.name())")
@Tag(name = "Restaurant Services Provider", description = "APIs per gestione ristoranti e prenotazioni per il profilo del fornitore")
public class ResturantProviderController {
    private final RestaurantService restaurantService;
    @Value("${hogu.client.url}")
    private String clientUrl;
    
    
    @GetMapping("/get-info")
    @Operation(summary = "Statistiche ristorante", description = "Restituisce le statistiche del ristorante del fornitore autenticato")
    public ResponseEntity<InfoStatsDto> getInfo(
            @Parameter(hidden = true)
            @AuthenticationPrincipal UserAccount userAccount
    ) {
        return ResponseEntity.ok(restaurantService.getInfo(userAccount.getAccountId()));
    }
    
    @GetMapping("/bookings-completed-commissions")
    @Operation(summary = "Commissioni prenotazioni completate", description = "Restituisce le prenotazioni COMPLETATE per calcolo commissioni")
    public ResponseEntity<Page<RestaurantBookingResponseDto>> getCompletedBookingsForCommissions(
            @Parameter(hidden = true) @AuthenticationPrincipal UserAccount userAccount,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(restaurantService.getCompletedBookingsForCommissions(userAccount.getAccountId(), pageable));
    }

    @PostMapping("/commissions/paypal")
    @Operation(summary = "Paga commissioni ristorante (PayPal)", description = "Crea una transazione PayPal per pagare tutte le commissioni dovute")
    public ResponseEntity<PaymentResponseDto> payRestaurantCommissions(
            @Parameter(hidden = true) @AuthenticationPrincipal UserAccount userAccount) {
        String base = clientUrl != null ? clientUrl : "";
        String url = base + "/provider/restaurant/commissions";
        
        PaymentResponseDto response = restaurantService.payRestaurantCommissions(userAccount.getAccountId(), url, url);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/booking/validate")
    @Operation(summary = "Valida codice prenotazione ristorante", description = "Verifica se il codice prenotazione è valido per oggi e appartiene al provider")
    public ResponseEntity<RestaurantBookingValidationResponseDto> validateBookingCode(
            @Parameter(hidden = true) @AuthenticationPrincipal UserAccount userAccount,
            @RequestParam String code
    ) {
        RestaurantBookingValidationResponseDto response = restaurantService.validateBookingByCode(userAccount.getAccountId(), code);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/commissions/paypal/execute")
    @Operation(summary = "Esegui pagamento commissioni (PayPal)", description = "Completa il pagamento PayPal delle commissioni")
    public ResponseEntity<PaymentResponseDto> executeRestaurantCommissionPayment(
            @Parameter(hidden = true) @AuthenticationPrincipal UserAccount userAccount,
            @RequestParam String paymentId,
            @RequestParam String payerId) {
        PaymentResponseDto response = restaurantService.executeRestaurantCommissionPayment(userAccount.getAccountId(), paymentId, payerId);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/commissions/stripe")
    @Operation(summary = "Paga commissioni ristorante (Stripe)", description = "Crea una sessione Stripe per pagare tutte le commissioni dovute")
    public ResponseEntity<PaymentResponseDto> payRestaurantCommissionsStripe(
            @Parameter(hidden = true) @AuthenticationPrincipal UserAccount userAccount) {
        String base = clientUrl != null ? clientUrl : "";
        String url = base + "/provider/restaurant/commissions";
        
        PaymentResponseDto response = restaurantService.payRestaurantCommissionsStripe(userAccount.getAccountId(), url, url);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/commissions/stripe/execute")
    @Operation(summary = "Esegui pagamento commissioni (Stripe)", description = "Completa il pagamento Stripe delle commissioni")
    public ResponseEntity<PaymentResponseDto> executeRestaurantCommissionPaymentStripe(
            @Parameter(hidden = true) @AuthenticationPrincipal UserAccount userAccount,
            @RequestParam String paymentId) {
        PaymentResponseDto response = restaurantService.executeRestaurantCommissionPaymentStripe(userAccount.getAccountId(), paymentId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{serviceId}")
    @Operation(summary = "Dettagli ristorante per modifica", description = "Recupera tutti i dati di un ristorante per la pagina di modifica (solo proprietario)")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Dettagli recuperati"),
        @ApiResponse(responseCode = "403", description = "Non autorizzato"),
        @ApiResponse(responseCode = "404", description = "Ristorante non trovato")
    })
    public ResponseEntity<RestaurantServiceDetailResponseDto> getRestaurantServiceForEdit(
            @Parameter(hidden = true)
            @AuthenticationPrincipal UserAccount userAccount,
            @PathVariable Long serviceId) {

    	RestaurantServiceDetailResponseDto response = restaurantService.getRestaurantServiceByIdAndProvider(
                serviceId, userAccount.getAccountId());

        return ResponseEntity.ok(response);
    }
    
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

    @GetMapping("/{id}/bookings-pending")
    @Operation(summary = "Prenotazioni ristorante in attesa", description = "Restituisce le prenotazioni IN ATTESA ricevute per un ristorante (solo proprietario)")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Prenotazioni recuperate con successo"),
        @ApiResponse(responseCode = "401", description = "Utente non autenticato o non autorizzato"),
        @ApiResponse(responseCode = "404", description = "Ristorante non trovato")
    })
    public ResponseEntity<Page<RestaurantBookingResponseDto>> getRestaurantBookingsPending(
            @Parameter(hidden = true)
            @AuthenticationPrincipal UserAccount userAccount,
            @Parameter(description = "ID del ristorante") @PathVariable Long id,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("creationDate").descending());

        Page<RestaurantBookingResponseDto> response = restaurantService.getRestaurantBookingsPending(id, userAccount.getAccountId(), pageable);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}/bookings-history")
    @Operation(summary = "Archivio Prenotazioni Ristorante", description = "Restituisce le prenotazioni passate (data < oggi)")
    public ResponseEntity<Page<RestaurantBookingResponseDto>> getRestaurantBookingsHistory(
            @Parameter(hidden = true) @AuthenticationPrincipal UserAccount userAccount,
            @PathVariable Long id,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        Pageable pageable = PageRequest.of(page, size); // Ordinamento già gestito nella query (DESC)
        return ResponseEntity.ok(restaurantService.getRestaurantBookingsHistory(id, userAccount.getAccountId(), pageable));
    }

    @GetMapping("/{id}/bookings-upcoming")
    @Operation(summary = "Prenotazioni Future Ristorante", description = "Restituisce le prenotazioni future o odierne (data >= oggi)")
    public ResponseEntity<Page<RestaurantBookingResponseDto>> getRestaurantBookingsUpcoming(
            @Parameter(hidden = true) @AuthenticationPrincipal UserAccount userAccount,
            @PathVariable Long id,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        Pageable pageable = PageRequest.of(page, size); // Ordinamento già gestito nella query (ASC)
        return ResponseEntity.ok(restaurantService.getRestaurantBookingsUpcoming(id, userAccount.getAccountId(), pageable));
    }

    @PostMapping("/booking/{bookingId}/confirm")
    @Operation(summary = "Conferma prenotazione", description = "Accetta una prenotazione ristorante")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "204", description = "Prenotazione confermata con successo"),
        @ApiResponse(responseCode = "404", description = "Prenotazione non trovata o non autorizzata"),
        @ApiResponse(responseCode = "400", description = "Stato non valido")
    })
    public ResponseEntity<Void> confirmBooking(
            @Parameter(hidden = true) @AuthenticationPrincipal UserAccount userAccount,
            @Parameter(description = "ID della prenotazione") @PathVariable Long bookingId) {
        
        restaurantService.acceptBooking(userAccount.getAccountId(), bookingId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/booking/{bookingId}")
    @Operation(summary = "Cancella prenotazione", description = "Cancella/Rifiuta una prenotazione ristorante")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "204", description = "Prenotazione cancellata con successo"),
        @ApiResponse(responseCode = "404", description = "Prenotazione non trovata o non autorizzata")
    })
    public ResponseEntity<Void> cancelBooking(
            @Parameter(hidden = true) @AuthenticationPrincipal UserAccount userAccount,
            @Parameter(description = "ID della prenotazione") @PathVariable Long bookingId,
            @Parameter(description = "Motivazione cancellazione") @RequestParam(required = false) String reason) {
        
        restaurantService.cancelBooking(userAccount.getAccountId(), bookingId, reason);
        return ResponseEntity.noContent().build();
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
