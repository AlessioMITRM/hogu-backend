package us.hogu.controller.provider;

import java.io.IOException;
import java.util.Date;
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
import us.hogu.common.constants.SuccessConstants;
import us.hogu.configuration.security.dto.UserAccount;
import us.hogu.controller.dto.request.BnbRoomPriceRequestDto;
import us.hogu.controller.dto.request.BnbRoomRequestDto;
import us.hogu.controller.dto.request.BnbServiceRequestDto;
import us.hogu.controller.dto.response.BnbBookingResponseDto;
import us.hogu.controller.dto.response.BnbRoomResponseDto;
import us.hogu.controller.dto.response.BnbServiceResponseDto;
import us.hogu.controller.dto.response.ClubBookingResponseDto;
import us.hogu.controller.dto.response.OperationResponseDto;
import us.hogu.service.intefaces.BnbService;

@RestController
@RequestMapping("/api/services/bnb/provider")
@RequiredArgsConstructor
@Tag(name = "Bnb Services Provider", description = "APIs per gestione servizi BNB e prenotazioni per Fornitore")
@PreAuthorize("hasAnyRole(T(us.hogu.model.enums.UserRole).PROVIDER.name())")
public class BnbProviderController {
    private final BnbService bnbService;

    
    @Operation(summary = "Ottiene tutti i B&B pubblicati (paginato)", description = "Restituisce la lista paginata dei servizi B&B pubblicati per il provider.")
    @ApiResponse(responseCode = "200", description = "Lista servizi ottenuta con successo")
    @GetMapping("/published")
    public ResponseEntity<Page<BnbServiceResponseDto>> getAllBnbServicesByProvider(
            @Parameter(hidden = true)
            @AuthenticationPrincipal UserAccount userAccount,
            @Parameter(description = "Numero di pagina (0-based)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Dimensione della pagina") @RequestParam(defaultValue = "10") int size,
            @Parameter(description = "Campo di ordinamento") @RequestParam(defaultValue = "creationDate") String sortBy,
            @Parameter(description = "Direzione ordinamento (asc|desc)") @RequestParam(defaultValue = "desc") String direction
    ) {
        Pageable pageable = PageRequest.of(page, size,
            direction.equalsIgnoreCase("desc") ? Sort.by(sortBy).descending() : Sort.by(sortBy).ascending());

        Page<BnbServiceResponseDto> response = bnbService.getAllBnbServicesByProvider(userAccount.getAccountId(), pageable);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}/bookings")
    @Operation(summary = "Prenotazioni B&B (paginato)", description = "Restituisce le prenotazioni ricevute per un B&B (solo proprietario) con paginazione.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Prenotazioni recuperate con successo"),
        @ApiResponse(responseCode = "401", description = "Utente non autenticato o non autorizzato"),
        @ApiResponse(responseCode = "404", description = "B&B non trovato")
    })
    public ResponseEntity<Page<BnbBookingResponseDto>> getBnbBookings(
            @Parameter(hidden = true)
            @AuthenticationPrincipal UserAccount userAccount,
            @Parameter(description = "ID del B&B") @PathVariable Long id,
            @Parameter(description = "Numero di pagina (0-based)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Dimensione della pagina") @RequestParam(defaultValue = "10") int size,
            @Parameter(description = "Campo di ordinamento") @RequestParam(defaultValue = "checkInDate") String sortBy,
            @Parameter(description = "Direzione ordinamento (asc|desc)") @RequestParam(defaultValue = "desc") String direction
    ) {
        Pageable pageable = PageRequest.of(page, size,
            direction.equalsIgnoreCase("desc") ? Sort.by(sortBy).descending() : Sort.by(sortBy).ascending());

        Page<BnbBookingResponseDto> response = bnbService.getBookingsForProvider(userAccount, id, pageable);
        return ResponseEntity.ok(response);
    }
    
    // 🔹 CREA UN NUOVO SERVIZIO B&B
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Crea un nuovo servizio B&B", description = "Permette di creare un nuovo servizio B&B associato a un provider.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Servizio creato con successo"),
            @ApiResponse(responseCode = "404", description = "Fornitore non trovato")
    })
    public ResponseEntity<BnbServiceResponseDto> createBnbService(
            @Parameter(hidden = true)
            @AuthenticationPrincipal UserAccount userAccount,
            @RequestPart("data") @Valid BnbServiceRequestDto request,
            @RequestPart(value = "images", required = false) List<MultipartFile> images) 
    throws IOException 
    {
        return ResponseEntity.ok(bnbService.createBnbService(userAccount, request, images));
    }

    // 🔹 AGGIORNA UN SERVIZIO B&B
    @PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Aggiorna un servizio B&B", description = "Permette di aggiornare i dettagli di un servizio B&B esistente.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Servizio aggiornato con successo"),
            @ApiResponse(responseCode = "404", description = "Servizio non trovato")
    })
    public ResponseEntity<Object> updateBnbService(
            @Parameter(hidden = true)
            @AuthenticationPrincipal UserAccount userAccount,
            @PathVariable Long id,
            @RequestPart("data") @Valid BnbServiceRequestDto request,
            @RequestPart(value = "images", required = false) List<MultipartFile> images) 
    throws IOException 
    {
        return ResponseEntity.ok(bnbService.updateBnbService(id, userAccount, request, images));
    }

    // 🔹 AGGIUNGI CAMERA A UN SERVIZIO
    @PostMapping(value = "/create-room/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Aggiunge una camera a un servizio B&B", description = "Permette di aggiungere una nuova camera a un B&B.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Camera aggiunta con successo"),
            @ApiResponse(responseCode = "404", description = "Servizio non trovato")
    })
    public ResponseEntity<BnbRoomResponseDto> addRoomToService(
            @Parameter(hidden = true)
            @AuthenticationPrincipal UserAccount userAccount,
            @PathVariable Long id,
            @RequestPart("data") @Valid BnbRoomRequestDto request,
            @RequestPart(value = "images", required = false) List<MultipartFile> images) 
    throws Exception 
    {
        return ResponseEntity.ok(bnbService.addRoomToService(userAccount, id, request, images));
    }

    // 🔹 AGGIORNA CAMERA DI UN SERVIZIO
    @PutMapping(value = "/update-room/{serviceId}/{roomId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Aggiorna una camera di un servizio B&B", description = "Permette di modificare i dettagli di una camera esistente di un B&B.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Camera aggiornata con successo"),
            @ApiResponse(responseCode = "404", description = "Camera o servizio non trovato")
    })
    public ResponseEntity<Object> updateRoom(
            @Parameter(hidden = true)
            @AuthenticationPrincipal UserAccount userAccount,
            @PathVariable Long serviceId,
            @PathVariable Long roomId,
            @RequestPart("data") @Valid BnbRoomRequestDto request,
            @RequestPart(value = "images", required = false) List<MultipartFile> images)
    throws Exception 
    {
        return ResponseEntity.ok(bnbService.updateRoom(userAccount, serviceId, roomId, request, images));
    }

    // 🔹 AGGIUNGI PERIODO DI PREZZO AD UNA CAMERA
    @PostMapping("/rooms/{roomId}/prices")
    @Operation(summary = "Aggiunge un periodo di prezzo a una camera", description = "Permette di configurare prezzi stagionali o specifici per una camera.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Prezzo aggiunto con successo"),
            @ApiResponse(responseCode = "404", description = "Camera non trovata")
    })
    public ResponseEntity<OperationResponseDto> addRoomPrice(
    	    @Parameter(hidden = true)
            @AuthenticationPrincipal UserAccount userAccount,
            @PathVariable Long roomId,
            @RequestBody BnbRoomPriceRequestDto dto) 
    {
        bnbService.addRoomPrice(userAccount, roomId, dto);
        
    	return ResponseEntity.ok(new OperationResponseDto(SuccessConstants.SUCCESS.name(), 
				 SuccessConstants.GENERAL_SUCCESS.getMessage(), new Date()));
    }
    
}
