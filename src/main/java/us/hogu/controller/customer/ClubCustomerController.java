package us.hogu.controller.customer;

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
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import us.hogu.configuration.security.dto.UserAccount;
import us.hogu.controller.dto.request.ClubBookingRequestDto;
import us.hogu.controller.dto.response.ClubBookingResponseDto;
import us.hogu.service.intefaces.ClubService;

@RestController
@RequestMapping("/api/services/club/customer")
@RequiredArgsConstructor
@Tag(name = "Clubs Services Customer", description = "APIs per gestione club e prenotazioni del Customer")
@PreAuthorize("hasAnyRole(T(us.hogu.model.enums.UserRole).PROVIDER.name())")
public class ClubCustomerController {
    private final ClubService clubService;

    
	@GetMapping("/bookings/my-bookings")
	@Operation(summary = "Le mie prenotazioni club (paginato)", description = "Restituisce in modo paginato le prenotazioni club dell'utente autenticato")
	public ResponseEntity<Page<ClubBookingResponseDto>> getUserClubBookings(	
	        @Parameter(hidden = true)
	        @AuthenticationPrincipal UserAccount userAccount,
	        @RequestParam(defaultValue = "0") int page,
	        @RequestParam(defaultValue = "10") int size,
	        @RequestParam(defaultValue = "creationDate") String sortBy,
	        @RequestParam(defaultValue = "desc") String direction
	) 
	{
	    Pageable pageable = PageRequest.of(
	        page, size,
	        direction.equalsIgnoreCase("desc") ? Sort.by(sortBy).descending() : Sort.by(sortBy).ascending()
	    );

	    Page<ClubBookingResponseDto> response = clubService.getUserClubBookings(userAccount.getAccountId(), pageable);
	    return ResponseEntity.ok(response);
	}
    
    @PostMapping("/{id}/bookings")
    @Operation(summary = "Crea prenotazione club", description = "Crea una nuova prenotazione per il club specificato")
    public ResponseEntity<ClubBookingResponseDto> createClubBooking(
    		@Parameter(hidden = true)
	        @AuthenticationPrincipal UserAccount userAccount,
            @Parameter(description = "ID del club") @PathVariable Long id,
            @Valid @RequestBody ClubBookingRequestDto requestDto) 
    {
        ClubBookingResponseDto response = clubService.createClubBooking(requestDto, userAccount.getAccountId());
        
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

}
