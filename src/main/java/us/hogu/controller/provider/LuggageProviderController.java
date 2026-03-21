package us.hogu.controller.provider;

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
import org.springframework.web.bind.annotation.DeleteMapping;
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
import us.hogu.common.constants.ErrorConstants;
import us.hogu.configuration.security.dto.UserAccount;
import us.hogu.controller.dto.request.LuggageServiceRequestDto;
import us.hogu.controller.dto.response.ClubInfoStatsDto;
import us.hogu.controller.dto.response.InfoStatsDto;
import us.hogu.controller.dto.response.LuggageBookingResponseDto;
import us.hogu.controller.dto.response.LuggageBookingValidationResponseDto;
import us.hogu.controller.dto.response.LuggageServiceDetailResponseDto;
import us.hogu.controller.dto.response.LuggageServiceProviderResponseDto;
import us.hogu.controller.dto.response.ServiceDetailResponseDto;
import us.hogu.controller.dto.response.ServiceSummaryResponseDto;
import us.hogu.exception.ValidationException;
import us.hogu.model.enums.ServiceType;
import us.hogu.service.intefaces.LuggageService;
import us.hogu.service.intefaces.PaymentService;

@RestController
@RequestMapping("/api/provider/services/luggage")
@RequiredArgsConstructor
@Tag(name = "Luggage Services Provider", description = "APIs per gestione servizi deposito bagagli per Fornitore")
@PreAuthorize("hasRole('PROVIDER')")
public class LuggageProviderController {

    private final LuggageService luggageService;
    private final PaymentService paymentService;


    @GetMapping("/get-info")
    @Operation(summary = "Statistiche deposito bagagli", description = "Restituisce le statistiche del deposito bagagli del provider autenticato")
    public ResponseEntity<InfoStatsDto> getInfo(
            @Parameter(hidden = true)
            @AuthenticationPrincipal UserAccount userAccount
    ) {
        return ResponseEntity.ok(luggageService.getInfo(userAccount.getAccountId()));
    }

    /**
     * RECUPERO singolo deposito per modifica (frontend edit page)
     */
    @GetMapping("/{serviceId}")
    @Operation(summary = "Dettagli deposito per modifica", description = "Recupera tutti i dati di un deposito bagagli per la pagina di modifica (solo proprietario)")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Dettagli recuperati"),
        @ApiResponse(responseCode = "403", description = "Non autorizzato"),
        @ApiResponse(responseCode = "404", description = "Deposito non trovato")
    })
    public ResponseEntity<LuggageServiceDetailResponseDto> getLuggageServiceForEdit(
            @AuthenticationPrincipal UserAccount userAccount,
            @PathVariable Long serviceId) {

    	LuggageServiceDetailResponseDto response = luggageService.getLuggageServiceByIdAndProvider(
                serviceId, userAccount.getAccountId());

        return ResponseEntity.ok(response);
    }

    /**
     * Lista paginata di tutti i depositi bagagli del provider autenticato
     */
    @GetMapping
    @Operation(summary = "Lista depositi bagagli del provider", description = "Restituisce la lista paginata di tutti i depositi bagagli del provider autenticato")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Lista recuperata con successo")
    })
    public ResponseEntity<Page<ServiceSummaryResponseDto>> getAllLuggageServicesByProvider(
            @AuthenticationPrincipal UserAccount userAccount,
            @Parameter(description = "Pagina (0-based)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Elementi per pagina") @RequestParam(defaultValue = "10") int size,
            @Parameter(description = "Campo di ordinamento") @RequestParam(defaultValue = "creationDate") String sortBy,
            @Parameter(description = "Direzione (asc/desc)") @RequestParam(defaultValue = "desc") String direction) {

        Pageable pageable = PageRequest.of(page, size,
                direction.equalsIgnoreCase("desc") ? Sort.by(sortBy).descending() : Sort.by(sortBy).ascending());

        Page<ServiceSummaryResponseDto> response = luggageService.getAllLuggageServicesByProvider(userAccount.getAccountId(), pageable);
        return ResponseEntity.ok(response);
    }

    /**
     * Prenotazioni ricevute per un deposito specifico (solo proprietario)
     */
    @GetMapping("/{serviceId}/bookings")
    @Operation(summary = "Prenotazioni ricevute", description = "Restituisce le prenotazioni per un deposito bagagli (solo proprietario)")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Prenotazioni recuperate"),
        @ApiResponse(responseCode = "403", description = "Accesso negato"),
        @ApiResponse(responseCode = "404", description = "Deposito non trovato")
    })
    public ResponseEntity<Page<LuggageBookingResponseDto>> getLuggageBookings(
            @AuthenticationPrincipal UserAccount userAccount,
            @PathVariable Long serviceId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "creationDate") String sortBy,
            @RequestParam(defaultValue = "desc") String direction) {

        Pageable pageable = PageRequest.of(page, size,
                direction.equalsIgnoreCase("desc") ? Sort.by(sortBy).descending() : Sort.by(sortBy).ascending());

        Page<LuggageBookingResponseDto> response = luggageService.getLuggageBookings(serviceId, userAccount.getAccountId(), pageable);
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/booking/validate")
    @Operation(summary = "Valida codice prenotazione Luggage", description = "Verifica se il codice prenotazione è valido e appartiene al provider")
    public ResponseEntity<LuggageBookingValidationResponseDto> validateLuggageBookingCode(
            @Parameter(hidden = true) @AuthenticationPrincipal UserAccount userAccount,
            @RequestParam String code
    ) {
        LuggageBookingValidationResponseDto response = luggageService.validateLuggageBookingByCode(userAccount.getAccountId(), code);
        if (!response.isValid()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
        }
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/{serviceId}/bookings-history")
    @Operation(summary = "Archivio prenotazioni deposito", description = "Restituisce le prenotazioni passate (fino a ieri) per un deposito bagagli")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Prenotazioni recuperate"),
        @ApiResponse(responseCode = "403", description = "Accesso negato"),
        @ApiResponse(responseCode = "404", description = "Deposito non trovato")
    })
    public ResponseEntity<Page<LuggageBookingResponseDto>> getLuggageBookingsHistory(
            @AuthenticationPrincipal UserAccount userAccount,
            @PathVariable Long serviceId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("dropOffTime").descending());
        Page<LuggageBookingResponseDto> response = luggageService.getLuggageBookingsHistory(serviceId, userAccount.getAccountId(), pageable);
        return ResponseEntity.ok(response);
    }

    /**
     * CREAZIONE nuovo deposito bagagli
     */
    @PostMapping(value = "/create", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Crea un nuovo deposito bagagli", description = "Crea un nuovo punto deposito. Le immagini sono obbligatorie.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Deposito creato con successo"),
        @ApiResponse(responseCode = "400", description = "Dati non validi o immagini mancanti")
    })
    public ResponseEntity<LuggageServiceDetailResponseDto> createLuggageService(
            @AuthenticationPrincipal UserAccount userAccount,
            @RequestPart("data") @Valid LuggageServiceRequestDto requestDto,
            @RequestPart("images") List<MultipartFile> images) {

        if (images == null || images.isEmpty()) {
            throw new ValidationException(ErrorConstants.IMAGES_REQUIRED.name(),
                    "Almeno un'immagine è obbligatoria per creare un deposito bagagli.");
        }

   /*     LuggageServiceDetailResponseDto response = luggageService.createLuggageService(
                userAccount.getAccountId(), requestDto, images);*/

        return ResponseEntity.status(HttpStatus.CREATED).body(null);
    }

    /**
     * AGGIORNAMENTO deposito bagagli esistente
     * Sostituisce completamente le immagini
     * @throws Exception 
     */
    @PutMapping(value = "/{serviceId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Aggiorna un deposito bagagli", description = "Aggiorna i dati del deposito. Devi reinviare tutte le immagini desiderate (sostituzione totale).")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Deposito aggiornato con successo"),
        @ApiResponse(responseCode = "400", description = "Dati non validi o immagini mancanti"),
        @ApiResponse(responseCode = "403", description = "Non autorizzato"),
        @ApiResponse(responseCode = "404", description = "Deposito non trovato")
    })
    public ResponseEntity<LuggageServiceDetailResponseDto> updateLuggageService(
            @AuthenticationPrincipal UserAccount userAccount,
            @PathVariable Long serviceId,
            @RequestPart("data") @Valid LuggageServiceRequestDto requestDto,
            @RequestPart("images") List<MultipartFile> images) throws Exception {


        LuggageServiceDetailResponseDto response = luggageService.updateLuggageService(
                userAccount.getAccountId(), serviceId, requestDto, images);

        return ResponseEntity.ok(response);
    }

    @PutMapping("/booking/{bookingId}/accept")
    @Operation(summary = "Accetta prenotazione bagagli", description = "Accetta una prenotazione Luggage e cattura il pagamento (se applicabile)")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "204", description = "Prenotazione accettata con successo"),
        @ApiResponse(responseCode = "404", description = "Prenotazione non trovata o non autorizzata"),
        @ApiResponse(responseCode = "400", description = "Errore nel pagamento o stato non valido")
    })
    public ResponseEntity<Void> acceptBooking(
            @Parameter(hidden = true) @AuthenticationPrincipal UserAccount userAccount,
            @Parameter(description = "ID della prenotazione") @PathVariable Long bookingId) {

        paymentService.confirmBookingByProvider(bookingId, ServiceType.LUGGAGE, userAccount.getAccountId());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/booking/{bookingId}/confirm")
    @Operation(summary = "Accetta prenotazione bagagli (Confirm)", description = "Accetta una prenotazione Luggage e cattura il pagamento (se applicabile)")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "204", description = "Prenotazione accettata con successo"),
        @ApiResponse(responseCode = "404", description = "Prenotazione non trovata o non autorizzata"),
        @ApiResponse(responseCode = "400", description = "Errore nel pagamento o stato non valido")
    })
    public ResponseEntity<Void> confirmBooking(
            @Parameter(hidden = true) @AuthenticationPrincipal UserAccount userAccount,
            @Parameter(description = "ID della prenotazione") @PathVariable Long bookingId) {

        paymentService.confirmBookingByProvider(bookingId, ServiceType.LUGGAGE, userAccount.getAccountId());
        return ResponseEntity.noContent().build();
    }

    public static class CancelRequest {
        private String reason;

        public String getReason() {
            return reason;
        }

        public void setReason(String reason) {
            this.reason = reason;
        }
    }

    @PutMapping("/booking/{bookingId}/reject")
    @Operation(summary = "Annulla prenotazione bagagli", description = "Annulla una prenotazione Luggage ed esegue il void/refund del pagamento se necessario")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "204", description = "Prenotazione annullata con successo"),
        @ApiResponse(responseCode = "404", description = "Prenotazione non trovata o non autorizzata")
    })
    public ResponseEntity<Void> rejectBooking(
            @Parameter(hidden = true) @AuthenticationPrincipal UserAccount userAccount,
            @Parameter(description = "ID della prenotazione") @PathVariable Long bookingId,
            @RequestBody(required = false) CancelRequest request) {

        String reason = request != null ? request.getReason() : null;
        paymentService.cancelBookingByProvider(bookingId, ServiceType.LUGGAGE, userAccount.getAccountId(), reason);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/booking/{bookingId}")
    @Operation(summary = "Annulla/Elimina prenotazione bagagli", description = "Annulla o elimina una prenotazione Luggage ed esegue il void/refund del pagamento se necessario")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "204", description = "Prenotazione annullata/eliminata con successo"),
        @ApiResponse(responseCode = "404", description = "Prenotazione non trovata o non autorizzata")
    })
    public ResponseEntity<Void> deleteBooking(
            @Parameter(hidden = true) @AuthenticationPrincipal UserAccount userAccount,
            @Parameter(description = "ID della prenotazione") @PathVariable Long bookingId,
            @RequestParam(required = false) String reason) {

        paymentService.cancelBookingByProvider(bookingId, ServiceType.LUGGAGE, userAccount.getAccountId(), reason);
        return ResponseEntity.noContent().build();
    }


}
