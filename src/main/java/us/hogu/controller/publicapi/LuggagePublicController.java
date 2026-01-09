package us.hogu.controller.publicapi;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
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
import us.hogu.controller.dto.request.LuggageSearchRequestDto;
import us.hogu.controller.dto.response.LuggageSearchResultResponseDto;
import us.hogu.controller.dto.response.LuggageServiceResponseDto;
import us.hogu.controller.dto.response.ServiceSummaryResponseDto;
import us.hogu.service.intefaces.LuggageService;

@RestController
@RequestMapping("/api/public/services/luggage")
@RequiredArgsConstructor
@Tag(name = "Luggage Services Public", description = "API pubbliche per consultare servizi LUGGAGE senza autenticazione")
public class LuggagePublicController {
    private final LuggageService luggageService;
	
    
    @GetMapping
    @Operation(summary = "Lista servizi bagagli attivi", description = "Restituisce la lista di tutti i servizi bagagli pubblici e attivi (paginata)")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Lista servizi bagagli recuperata con successo")
    })
    public ResponseEntity<Page<ServiceSummaryResponseDto>> getAllActiveLuggageServices(
            @Parameter(hidden = true) Pageable pageable) {
        Page<ServiceSummaryResponseDto> response = luggageService.getAllActiveLuggageServices(pageable);
        return ResponseEntity.ok(response);
    }
    
    @GetMapping(value = "/{id}")
    @Operation(summary = "Dettaglio provider", description = "Restituisce il dettaglio di un provider bagagli")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Dettaglio di un provider bagagli recuperata con successo")
    })
    public ResponseEntity<LuggageServiceResponseDto> getDetailLuggageServices(
            @Parameter(description = "ID del servizio bagagli") @PathVariable Long id) 
    {
        return ResponseEntity.ok(luggageService.getLuggageServiceDetail(id));
    }
    
    @PostMapping("/search")
    public ResponseEntity<Page<LuggageSearchResultResponseDto>> searchLuggage(@RequestBody LuggageSearchRequestDto request) 
    {
        // Default paginazione se null
        int page = request.getPage() != null ? request.getPage() : 0;
        int size = request.getSize() != null ? request.getSize() : 10;
        
        Pageable pageable = PageRequest.of(page, size);
        
        return ResponseEntity.ok(luggageService.searchNative(request, pageable));
    }
    
    
    
}
