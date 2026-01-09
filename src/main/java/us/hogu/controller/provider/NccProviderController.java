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
import us.hogu.controller.dto.response.BnbBookingResponseDto;
import us.hogu.controller.dto.response.NccManagementResponseDto;
import us.hogu.controller.dto.response.ServiceDetailResponseDto;
import us.hogu.repository.projection.NccManagementProjection;
import us.hogu.service.intefaces.NccService;

@RestController
@RequestMapping("/api/services/ncc/provider")
@RequiredArgsConstructor
@Tag(name = "NCC Services Provider", description = "APIs per gestione servizi NCC e prenotazioni per Fornitore")
@PreAuthorize("hasAnyRole(T(us.hogu.model.enums.UserRole).PROVIDER.name())")
public class NccProviderController {
    private final NccService nccService;


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
    public ResponseEntity<List<BnbBookingResponseDto>> getNccBookings(
    		@Parameter(hidden = true)
	        @AuthenticationPrincipal UserAccount userAccount,
            @Parameter(description = "ID del NCC") @PathVariable Long id) 
    {        
    	return null;
        //return ResponseEntity.ok(nccService.getNccBookings(userAccount));
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
    public ResponseEntity<ServiceDetailResponseDto> updateNccService(
            @Parameter(hidden = true)
            @AuthenticationPrincipal UserAccount userAccount,
            @Parameter(description = "ID del servizio NCC") @PathVariable Long id,
            @RequestPart("data") @Valid NccServiceRequestDto requestDto,
            @RequestPart(value = "images", required = false) List<MultipartFile> images) 
    throws Exception 
    {
        ServiceDetailResponseDto response = nccService.updateNccService(userAccount.getAccountId(), id, requestDto, images);
        
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
