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
import us.hogu.configuration.security.dto.UserAccount;
import us.hogu.controller.dto.request.NccServiceRequestDto;
import us.hogu.controller.dto.response.NccBookingResponseDto;
import us.hogu.controller.dto.response.NccBookingValidationResponseDto;
import us.hogu.controller.dto.response.InfoStatsDto;
import us.hogu.controller.dto.response.LuggageServiceDetailResponseDto;
import us.hogu.controller.dto.response.NccDetailResponseDto;
import us.hogu.controller.dto.response.NccManagementResponseDto;
import us.hogu.controller.dto.response.ServiceDetailResponseDto;
import us.hogu.repository.projection.NccManagementProjection;
import us.hogu.model.enums.ServiceType;
import us.hogu.service.intefaces.PaymentService;
import us.hogu.service.intefaces.NccService;

import java.math.BigDecimal;

@RestController
@RequestMapping("/api/provider/services/ncc")
@RequiredArgsConstructor
@Tag(name = "NCC Services Provider", description = "APIs per gestione servizi NCC e prenotazioni per Fornitore")
@PreAuthorize("hasAnyRole(T(us.hogu.model.enums.UserRole).PROVIDER.name())")
public class NccProviderController {
    private final NccService nccService;
    private final PaymentService paymentService;


    @GetMapping("/get-info")
    @Operation(summary = "Statistiche deposito bagagli", description = "Restituisce le statistiche del deposito bagagli del provider autenticato")
    public ResponseEntity<InfoStatsDto> getInfo(
            @Parameter(hidden = true)
            @AuthenticationPrincipal UserAccount userAccount
    ) {
        return ResponseEntity.ok(nccService.getInfo(userAccount.getAccountId()));
    }
    
    @GetMapping("/{serviceId}")
    @Operation(summary = "Dettagli deposito per modifica", description = "Recupera tutti i dati di un deposito bagagli per la pagina di modifica (solo proprietario)")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Dettagli recuperati"),
        @ApiResponse(responseCode = "403", description = "Non autorizzato"),
        @ApiResponse(responseCode = "404", description = "Deposito non trovato")
    })
    public ResponseEntity<NccDetailResponseDto> getLuggageServiceForEdit(
            @AuthenticationPrincipal UserAccount userAccount,
            @PathVariable Long serviceId) {

    	NccDetailResponseDto response = nccService.getNccServiceByServiceIdAndProviderId(
                serviceId, userAccount.getAccountId());

        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/my-services")
    @Operation(summary = "I miei servizi NCC (paginati)", description = "Restituisce la lista dei servizi NCC del fornitore autenticato con supporto alla paginazione")
    public ResponseEntity<Page<NccManagementResponseDto>> getProviderNccServices(
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

        Page<NccManagementResponseDto> response = nccService.getProviderNccServices(userAccount.getAccountId(), pageable);
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/{id}/bookings")
    @Operation(summary = "Prenotazioni NCC", description = "Restituisce le prenotazioni ricevute per un NCC (solo proprietario)")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Prenotazioni recuperate con successo"),
        @ApiResponse(responseCode = "401", description = "Utente non autenticato o non autorizzato"),
        @ApiResponse(responseCode = "404", description = "Ncc non trovato")
    })
    public ResponseEntity<List<NccBookingResponseDto>> getNccBookings(
            @Parameter(hidden = true)
            @AuthenticationPrincipal UserAccount userAccount,
            @Parameter(description = "ID del NCC") @PathVariable Long id)
    {
        List<NccBookingResponseDto> bookings = nccService.getNccBookings(id, userAccount.getAccountId());
        return ResponseEntity.ok(bookings);
    }

    @GetMapping("/{id}/bookings-history")
    @Operation(summary = "Storico prenotazioni NCC (paginato)", description = "Restituisce lo STORICO delle prenotazioni passate (fino a ieri) per un servizio NCC")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Prenotazioni recuperate con successo"),
        @ApiResponse(responseCode = "401", description = "Utente non autenticato o non autorizzato"),
        @ApiResponse(responseCode = "404", description = "NCC non trovato")
    })
    public ResponseEntity<Page<NccBookingResponseDto>> getNccBookingsHistory(
            @Parameter(hidden = true)
            @AuthenticationPrincipal UserAccount userAccount,
            @Parameter(description = "ID del NCC") @PathVariable Long id,
            @Parameter(description = "Numero di pagina (0-based)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Dimensione della pagina") @RequestParam(defaultValue = "10") int size
    ) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("pickupTime").descending());
        Page<NccBookingResponseDto> response = nccService.getNccBookingsHistory(id, userAccount.getAccountId(), pageable);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}/bookings/fully-paid")
    @Operation(summary = "Prenotazioni NCC con pagamento completato", description = "Restituisce le prenotazioni NCC con stato PAGAMENTO_COMPLETO_ESEGUITO per un servizio (solo proprietario)")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Prenotazioni recuperate con successo"),
        @ApiResponse(responseCode = "401", description = "Utente non autenticato o non autorizzato"),
        @ApiResponse(responseCode = "404", description = "Ncc non trovato")
    })
    public ResponseEntity<List<NccBookingResponseDto>> getNccFullyPaidBookings(
            @Parameter(hidden = true)
            @AuthenticationPrincipal UserAccount userAccount,
            @Parameter(description = "ID del NCC") @PathVariable Long id)
    {
        List<NccBookingResponseDto> bookings = nccService.getNccFullyPaidBookings(id, userAccount.getAccountId());
        return ResponseEntity.ok(bookings);
    }

    @GetMapping("/{id}/bookings/current")
    @Operation(summary = "Prenotazione NCC corrente", description = "Restituisce la prenotazione NCC corrente (oggi, pagamento completo) per un servizio, se presente")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Prenotazione corrente recuperata con successo (o null se non presente)"),
        @ApiResponse(responseCode = "401", description = "Utente non autenticato o non autorizzato"),
        @ApiResponse(responseCode = "404", description = "Ncc non trovato")
    })
    public ResponseEntity<NccBookingResponseDto> getCurrentNccBooking(
            @Parameter(hidden = true)
            @AuthenticationPrincipal UserAccount userAccount,
            @Parameter(description = "ID del NCC") @PathVariable Long id)
    {
        NccBookingResponseDto booking = nccService.getCurrentNccBooking(id, userAccount.getAccountId());
        return ResponseEntity.ok(booking);
    }

    @GetMapping("/booking/validate")
    @Operation(summary = "Valida codice prenotazione NCC", description = "Verifica se il codice prenotazione è valido per oggi e appartiene al provider")
    public ResponseEntity<NccBookingValidationResponseDto> validateBookingCode(
            @Parameter(hidden = true) @AuthenticationPrincipal UserAccount userAccount,
            @RequestParam String code
    ) {
        NccBookingValidationResponseDto response = nccService.validateNccBookingByCode(userAccount.getAccountId(), code);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/booking/{bookingId}")
    @Operation(summary = "Cancella prenotazione", description = "Cancella una prenotazione NCC effettuata")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "204", description = "Prenotazione cancellata con successo"),
        @ApiResponse(responseCode = "404", description = "Prenotazione non trovata o non autorizzata")
    })
    public ResponseEntity<Void> cancelBooking(
            @Parameter(hidden = true) @AuthenticationPrincipal UserAccount userAccount,
            @Parameter(description = "ID della prenotazione") @PathVariable Long bookingId,
            @Parameter(description = "Motivazione cancellazione") @RequestParam(required = false) String reason) {
        
        paymentService.cancelBookingByProvider(bookingId, ServiceType.NCC, userAccount.getAccountId(), reason);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/booking/{bookingId}/accept")
    @Operation(summary = "Accetta prenotazione", description = "Accetta una prenotazione NCC e cattura il pagamento (se applicabile)")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "204", description = "Prenotazione accettata con successo"),
        @ApiResponse(responseCode = "404", description = "Prenotazione non trovata o non autorizzata"),
        @ApiResponse(responseCode = "400", description = "Errore nel pagamento o stato non valido")
    })
    public ResponseEntity<Void> acceptBooking(
            @Parameter(hidden = true) @AuthenticationPrincipal UserAccount userAccount,
            @Parameter(description = "ID della prenotazione") @PathVariable Long bookingId) {
        
        paymentService.confirmBookingByProvider(bookingId, ServiceType.NCC, userAccount.getAccountId());
        return ResponseEntity.noContent().build();
    }
    
    @PutMapping("/booking/{bookingId}/rectify")
    @Operation(summary = "Rettifica prezzo prenotazione", description = "Propone una rettifica del prezzo per una prenotazione NCC e la imposta in attesa di pagamento cliente")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "204", description = "Rettifica inviata con successo"),
        @ApiResponse(responseCode = "404", description = "Prenotazione non trovata o non autorizzata"),
        @ApiResponse(responseCode = "400", description = "Dati non validi o stato non rettificabile")
    })
    public ResponseEntity<Void> rectifyBooking(
            @Parameter(hidden = true) @AuthenticationPrincipal UserAccount userAccount,
            @Parameter(description = "ID della prenotazione") @PathVariable Long bookingId,
            @RequestBody RectifyPriceRequest request) {

        BigDecimal parsedPrice = null;
        if (request != null && request.getPrice() != null) {
            String raw = request.getPrice().trim();
            if (!raw.isEmpty()) {
                String normalized = normalizePrice(raw);
                try {
                    parsedPrice = new BigDecimal(normalized);
                } catch (NumberFormatException e) {
                    parsedPrice = null;
                }
            }
        }

        String note = request != null ? request.getNote() : null;
        nccService.rectifyBooking(userAccount.getAccountId(), bookingId, parsedPrice, note);
        return ResponseEntity.noContent().build();
    }
    
    @PutMapping("/booking/{bookingId}/complete")
    @Operation(summary = "Completa prenotazione", description = "Segna una prenotazione NCC come completata")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "204", description = "Prenotazione completata con successo"),
        @ApiResponse(responseCode = "404", description = "Prenotazione non trovata o non autorizzata"),
        @ApiResponse(responseCode = "400", description = "Stato non valido")
    })
    public ResponseEntity<Void> completeBooking(
            @Parameter(hidden = true) @AuthenticationPrincipal UserAccount userAccount,
            @Parameter(description = "ID della prenotazione") @PathVariable Long bookingId) {
        
        paymentService.completeBookingByProvider(bookingId, ServiceType.NCC, userAccount.getAccountId());
        return ResponseEntity.noContent().build();
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ServiceDetailResponseDto> createNccService(
            @AuthenticationPrincipal UserAccount userAccount,
            @RequestPart("data") @Valid NccServiceRequestDto requestDto,
            @RequestPart("images") List<MultipartFile> images) throws Exception {

        ServiceDetailResponseDto response = nccService.createNccService(userAccount.getAccountId(), requestDto, images);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
    
    @PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Aggiorna servizio NCC", description = "Aggiorna un nuovo servizio NCC")
    public ResponseEntity<NccDetailResponseDto> updateNccService(
            @Parameter(hidden = true)
            @AuthenticationPrincipal UserAccount userAccount,
            @Parameter(description = "ID del servizio NCC") @PathVariable Long id,
            @RequestPart("service") @Valid NccServiceRequestDto requestDto,
            @RequestPart(value = "images", required = false) List<MultipartFile> images) 
    throws Exception 
    {
        NccDetailResponseDto response = nccService.updateNccService(userAccount.getAccountId(), id, requestDto, images);
        
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
    
    public static class RectifyPriceRequest {
        private String price;
        private String note;

        public String getPrice() {
            return price;
        }

        public void setPrice(String price) {
            this.price = price;
        }

        public String getNote() {
            return note;
        }

        public void setNote(String note) {
            this.note = note;
        }
    }

    private static String normalizePrice(String value) {
        String cleaned = value.replace("€", "").replace(" ", "");
        if (cleaned.isEmpty()) {
            return cleaned;
        }

        int lastDot = cleaned.lastIndexOf('.');
        int lastComma = cleaned.lastIndexOf(',');
        char decimalSeparator = 0;
        if (lastDot > lastComma && lastDot >= 0) {
            decimalSeparator = '.';
        } else if (lastComma > lastDot && lastComma >= 0) {
            decimalSeparator = ',';
        }

        StringBuilder result = new StringBuilder();
        for (int i = 0; i < cleaned.length(); i++) {
            char c = cleaned.charAt(i);
            if (Character.isDigit(c)) {
                result.append(c);
            } else if (decimalSeparator != 0 && c == decimalSeparator) {
                result.append('.');
            }
        }
        return result.toString();
    }
}
