package us.hogu.controller.customer;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import us.hogu.controller.dto.response.BnbBookingResponseDto;
import us.hogu.service.intefaces.BnbService;
import us.hogu.service.intefaces.ClubService;

@RestController
@RequestMapping("/api/services/bnb/customer")
@RequiredArgsConstructor
@Tag(name = "Bnb Services Customer", description = "APIs per gestione Bnb del Customer")
@PreAuthorize("hasAnyRole(T(us.hogu.model.enums.UserRole).CUSTOMER.name())")
public class BnbCustomerController {
    private final BnbService bnbService;

    
    // 🔹 LISTA PRENOTAZIONI DI UN UTENTE
    @Operation(summary = "Prenotazioni utente (paginato)", description = "Restituisce la lista delle prenotazioni effettuate da un determinato utente con supporto alla paginazione.")
    @ApiResponse(responseCode = "200", description = "Prenotazioni ottenute con successo")
    @GetMapping("/bookings/{userId}")
    public ResponseEntity<Page<BnbBookingResponseDto>> getBookingsForUser(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "bookingDate") String sortBy,
            @RequestParam(defaultValue = "desc") String direction
    ) {
        Pageable pageable = PageRequest.of(
            page, size,
            direction.equalsIgnoreCase("desc") ? Sort.by(sortBy).descending() : Sort.by(sortBy).ascending()
        );

        Page<BnbBookingResponseDto> response = bnbService.getBookingsForUser(userId, pageable);
        return ResponseEntity.ok(response);
    }

    
    // 🔹 CREA UNA PRENOTAZIONE
    @Operation(summary = "Crea una prenotazione", description = "Permette a un utente di prenotare una camera in un determinato periodo.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Prenotazione creata con successo"),
            @ApiResponse(responseCode = "404", description = "Utente o camera non trovata")
    })
    @PostMapping("/bookings")
    public ResponseEntity<BnbBookingResponseDto> createBooking(
            @RequestParam Long userId,
            @RequestParam Long roomId,
            @RequestParam LocalDate checkIn,
            @RequestParam LocalDate checkOut,
            @RequestParam Integer guests) {

        return ResponseEntity.ok(bnbService.createBooking(userId, roomId, checkIn, checkOut, guests));
    }

}
