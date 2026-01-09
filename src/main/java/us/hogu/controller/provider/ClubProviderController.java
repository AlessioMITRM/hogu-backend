package us.hogu.controller.provider;

import java.util.List;

import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Positive;
import javax.validation.constraints.PositiveOrZero;
import javax.validation.constraints.Size;

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
import us.hogu.controller.dto.request.ClubServiceRequestDto;
import us.hogu.controller.dto.request.EventClubServiceRequestDto;
import us.hogu.controller.dto.response.ClubBookingResponseDto;
import us.hogu.controller.dto.response.ClubManagementResponseDto;
import us.hogu.controller.dto.response.EventClubServiceResponseDto;
import us.hogu.controller.dto.response.InfoStatsDto;
import us.hogu.controller.dto.response.RestaurantBookingResponseDto;
import us.hogu.controller.dto.response.ServiceDetailResponseDto;
import us.hogu.model.EventClubServiceEntity;
import us.hogu.service.intefaces.ClubService;

@RestController
@RequestMapping("/api/provider/services/club")
@RequiredArgsConstructor
@Tag(name = "Clubs Services Provider", description = "APIs per gestione club e prenotazioni del Provider")
@PreAuthorize("hasAnyRole(T(us.hogu.model.enums.UserRole).PROVIDER.name())")
public class ClubProviderController {
    private final ClubService clubService;
	
    
    @GetMapping("/get-info")
    @Operation(summary = "Eventi del club (paginato)", description = "Restituisce gli eventi associati a un club con supporto alla paginazione")
    public ResponseEntity<InfoStatsDto> getInfo(
            @Parameter(hidden = true)
            @AuthenticationPrincipal UserAccount userAccount
    ) {
        return ResponseEntity.ok(clubService.getInfo(userAccount.getAccountId()));
    }
    
    @GetMapping("{id}/event/get-all")
    @Operation(summary = "Eventi del club (paginato)", description = "Restituisce gli eventi associati a un club con supporto alla paginazione")
    public ResponseEntity<Page<EventClubServiceResponseDto>> getEvents(
            @Parameter(hidden = true)
            @AuthenticationPrincipal UserAccount userAccount,
            @Parameter(description = "ID del club") @PathVariable Long id,
            @Parameter(description = "Numero di pagina (0-based)") @RequestParam(defaultValue = "0") int page
    ) 
    {
        Pageable pageable = PageRequest.of(page, 10,
            "desc".equalsIgnoreCase("desc") ? Sort.by("startTime").descending() : Sort.by("startTime").ascending());

        return ResponseEntity.ok(clubService.getEvents(userAccount.getAccountId(), id, pageable));
    }
    
    @GetMapping("{id}/event/get-all-today")
    @Operation(summary = "Eventi del club (paginato)", description = "Restituisce gli eventi associati a un club di oggi con supporto alla paginazione")
    public ResponseEntity<Page<EventClubServiceResponseDto>> getEventsToday(
            @Parameter(hidden = true)
            @AuthenticationPrincipal UserAccount userAccount,
            @Parameter(description = "ID del club") @PathVariable Long id,
            @Parameter(description = "Numero di pagina (0-based)") @RequestParam(defaultValue = "0") int page
    ) 
    {
        Pageable pageable = PageRequest.of(page, 10,
            "desc".equalsIgnoreCase("desc") ? Sort.by("startTime").descending() : Sort.by("startTime").ascending());

        return ResponseEntity.ok(clubService.getEventsToday(userAccount.getAccountId(), id, pageable));
    }
    
    @GetMapping("/{id}/bookings-pending")
    @Operation(summary = "Prenotazioni club (paginato)", description = "Restituisce le prenotazioni in attesa di un CLUB con supporto alla paginazione")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Prenotazioni recuperate con successo"),
        @ApiResponse(responseCode = "401", description = "Utente non autenticato o non autorizzato"),
        @ApiResponse(responseCode = "404", description = "Club non trovato")
    })
    public ResponseEntity<Page<ClubBookingResponseDto>> getClubBookingsPending(
            @Parameter(hidden = true)
            @AuthenticationPrincipal UserAccount userAccount,
            @Parameter(description = "ID del Club") @PathVariable Long id,
            @Parameter(description = "Numero di pagina (0-based)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Dimensione della pagina") @RequestParam(defaultValue = "10") int size
    ) 
    {
        Pageable pageable = PageRequest.of(page, size,
            "desc".equalsIgnoreCase("desc") ? Sort.by("reservationTime").descending() : Sort.by("reservationTime").ascending());

        Page<ClubBookingResponseDto> response = clubService.getClubBookingsPending(userAccount.getAccountId(), id, pageable);
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/{id}/bookings")
    @Operation(summary = "Prenotazioni club (paginato)", description = "Restituisce le prenotazioni ricevute per un CLUB (solo proprietario) con supporto alla paginazione")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Prenotazioni recuperate con successo"),
        @ApiResponse(responseCode = "401", description = "Utente non autenticato o non autorizzato"),
        @ApiResponse(responseCode = "404", description = "Club non trovato")
    })
    public ResponseEntity<Page<ClubBookingResponseDto>> getClubBookings(
            @Parameter(hidden = true)
            @AuthenticationPrincipal UserAccount userAccount,
            @Parameter(description = "ID del Club") @PathVariable Long id,
            @Parameter(description = "Numero di pagina (0-based)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Dimensione della pagina") @RequestParam(defaultValue = "10") int size
    ) 
    {
        Pageable pageable = PageRequest.of(page, size,
            "desc".equalsIgnoreCase("desc") ? Sort.by("reservationTime").descending() : Sort.by("reservationTime").ascending());

        Page<ClubBookingResponseDto> response = clubService.getClubBookings(userAccount.getAccountId(), id, pageable);
        return ResponseEntity.ok(response);
    }

    
    @GetMapping("club/get-club/{id}")
    @Operation(summary = "presa club", description = "Restituisce le informazioni di un club")
    public ResponseEntity<ClubManagementResponseDto> getProviderClub(	
    		@Parameter(hidden = true)
    		@AuthenticationPrincipal UserAccount userAccount,
            @Parameter(description = "ID del club") @PathVariable Long id) 
    {      
        return ResponseEntity.ok(clubService.getProviderClub(userAccount.getAccountId(), id));
    }
    
    @GetMapping("/event/get-event/{eventId}")
    @Operation(summary = "presa evento", description = "Restituisce il dettaglio di un evento")
    public ResponseEntity<EventClubServiceResponseDto> getEvent(	
    		@Parameter(hidden = true)
    		@AuthenticationPrincipal UserAccount userAccount,
    		@Parameter(description = "ID del evento") @PathVariable Long eventId) 
    {        
        return ResponseEntity.ok(clubService.getEventForProvider(userAccount.getAccountId(), eventId));
    }
    
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Crea nuovo club", description = "Crea un nuovo club con immagini opzionali")
    public ResponseEntity<ServiceDetailResponseDto> createClub(
            @Parameter(hidden = true)
            @AuthenticationPrincipal UserAccount userAccount,
            @RequestPart("data") @Valid ClubServiceRequestDto requestDto,
            @RequestPart(value = "images", required = false) List<MultipartFile> images) throws Exception 
    {
        return ResponseEntity.status(HttpStatus.CREATED).body(clubService.createClub(userAccount.getAccountId(),
        		requestDto, images));
    }
    
    @PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Aggiorna club", description = "Aggiorna un club esistente con immagini opzionali")
    public ResponseEntity<ServiceDetailResponseDto> updateClub(
            @Parameter(hidden = true)
            @AuthenticationPrincipal UserAccount userAccount,
            @Parameter(description = "ID del club") @PathVariable Long id,
            @RequestPart("data") @Valid ClubServiceRequestDto requestDto,
            @RequestPart(value = "images", required = false) List<MultipartFile> images) throws Exception {

        ServiceDetailResponseDto response = clubService.updateClub(userAccount.getAccountId(), id, requestDto, images);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
    
    
    @PostMapping(value = "/event/create", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Crea evento", description = "Crea un nuovo evento")
    public ResponseEntity<EventClubServiceResponseDto> createEvent(
            @Parameter(hidden = true) @AuthenticationPrincipal UserAccount userAccount,
            @RequestPart("data") @Valid EventClubServiceRequestDto requestDto,
            @RequestPart(value = "images", required = false) List<MultipartFile> images) throws Exception {

        return ResponseEntity.ok(clubService.createEvent(
                userAccount.getAccountId(),
                requestDto,
                images != null ? images : List.of()));
    }

    @PutMapping(value = "/event/{eventId}/update", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Aggiorna evento", description = "Aggiorna un evento esistente tramite il suo ID con immagini opzionali")
    public ResponseEntity<EventClubServiceResponseDto> updateEvent(
            @Parameter(hidden = true) @AuthenticationPrincipal UserAccount userAccount,
            @Parameter(description = "ID dell'evento") @PathVariable Long eventId,
            @RequestPart("data") @Valid EventClubServiceRequestDto requestDto,
            @RequestPart(value = "images", required = false) List<MultipartFile> images) throws Exception {

        return ResponseEntity.ok(clubService.updateEvent(
                userAccount.getAccountId(),
                eventId,
                requestDto,
                images != null ? images : List.of()));
    }
    
}
