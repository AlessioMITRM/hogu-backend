package us.hogu.controller.publicapi;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
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
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import us.hogu.controller.dto.request.EventSearchRequestDto;
import us.hogu.controller.dto.response.ClubServiceResponseDto;
import us.hogu.controller.dto.response.EventClubServiceResponseDto;
import us.hogu.controller.dto.response.ServiceSummaryResponseDto;
import us.hogu.service.intefaces.ClubService;

@RestController
@RequestMapping("/api/public/services/club")
@RequiredArgsConstructor
@Tag(name = "Clubs Services Public", description = "APIs pubbliche per consultare club ed eventi")
public class ClubPublicController {
    private final ClubService clubService;

    
    @GetMapping
    @Operation(summary = "Lista club attivi", description = "Restituisce la lista di tutti i club pubblici e attivi")
    public ResponseEntity<List<ServiceSummaryResponseDto>> getActiveClubs(
    		@RequestParam(required = false, defaultValue = "") String searchText) 
    {
        List<ServiceSummaryResponseDto> response = clubService.getActiveClubs(searchText);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/with-events")
    @Operation(summary = "Club con eventi attivi", description = "Restituisce i club che hanno eventi in programma")
    public ResponseEntity<List<ServiceSummaryResponseDto>> getClubsWithEvents() {
        List<ServiceSummaryResponseDto> response = clubService.getClubsWithEvents();
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Dettaglio club", description = "Restituisce i dettagli completi di un club")
    public ResponseEntity<ClubServiceResponseDto> getClubDetail(
            @Parameter(description = "ID del club") @PathVariable Long id) 
    {    
    	return ResponseEntity.ok(clubService.getClubDetail(id));
    }
    
    @GetMapping("/event/{eventId}")
    @Operation(summary = "presa evento", description = "Restituisce il dettaglio di un evento")
    public ResponseEntity<EventClubServiceResponseDto> getEvent(	
    		@Parameter(description = "ID del evento") @PathVariable Long eventId) 
    {        
        return ResponseEntity.ok(clubService.getEvent(eventId));
    }
    
    @PostMapping("/search")
    @Operation(
        summary = "Eventi del club (Ricerca Avanzata)",
        description = "Restituisce in modo paginato gli eventi filtrati. I filtri sono passati nel body, la paginazione nell'URL."
    )
    public ResponseEntity<Page<us.hogu.controller.dto.response.EventPublicResponseDto>> searchEvents(
            @RequestBody(required = false) EventSearchRequestDto request,             
            @PageableDefault(sort = "startTime", direction = Sort.Direction.ASC) Pageable pageable
    ) {
        // Gestione null safety se il body è vuoto
        if (request == null) {
            request = new EventSearchRequestDto(); 
        }

        // Chiamata al service (lo stesso che hai creato prima)
        Page<us.hogu.controller.dto.response.EventPublicResponseDto> response = 
            clubService.getEventsForPublicWithFilters(
                request.getLocation(), 
                request.getEventType(), 
                request.getDate(), 
                request.getTable(),
                pageable
            );

        return ResponseEntity.ok(response);   
    }
}
