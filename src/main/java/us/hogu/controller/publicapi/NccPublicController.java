package us.hogu.controller.publicapi;

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
import us.hogu.controller.dto.request.NccSearchRequestDto;
import us.hogu.controller.dto.request.NccServiceRequestDto;
import us.hogu.controller.dto.request.RestaurantAdvancedSearchRequestDto;
import us.hogu.controller.dto.response.NccBookingResponseDto;
import us.hogu.controller.dto.response.ServiceDetailResponseDto;
import us.hogu.controller.dto.response.ServiceSummaryResponseDto;
import us.hogu.repository.projection.NccManagementProjection;
import us.hogu.service.intefaces.NccService;

@RestController
@RequestMapping("/api/public/services/ncc")
@RequiredArgsConstructor
@Tag(name = "NCC Services Public", description = "API pubbliche per consultare servizi NCC senza autenticazione")
public class NccPublicController {
    private final NccService nccService;


    @GetMapping("/{id}")
    @Operation(summary = "Dettaglio servizio NCC", description = "Restituisce i dettagli completi di un servizio NCC")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Dettaglio servizio NCC recuperato con successo"),
        @ApiResponse(responseCode = "404", description = "Servizio NCC non trovato")
    })
    public ResponseEntity<ServiceDetailResponseDto> getNccServiceDetail(
            @Parameter(description = "ID del servizio NCC") @PathVariable Long id) 
    {
        ServiceDetailResponseDto response = nccService.getNccServiceDetail(id);
        
        return ResponseEntity.ok(response);
    }
    
    @PostMapping("/search")
    @Operation(summary = "Lista ricerca servizi NCC attivi (paginata)",
               description = "Restituisce la lista di tutti i servizi NCC pubblici e attivi, con ricerca e paginazione")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Lista servizi NCC recuperata con successo")
    })
    public ResponseEntity<Page<ServiceSummaryResponseDto>> getActiveNccServices(
    		@RequestBody NccSearchRequestDto searchRequest,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "basePrice,desc") String sort) 
    {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Order.desc(sort.split(",")[0])));
        Page<ServiceSummaryResponseDto> response = nccService.getActiveNccServices(searchRequest, pageable);
        return ResponseEntity.ok(response);
    }
    
}
