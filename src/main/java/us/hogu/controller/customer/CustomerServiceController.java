package us.hogu.controller.customer;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;

import us.hogu.service.intefaces.CustomerService;

@RestController
@RequestMapping("/api/customer/services")
@PreAuthorize("hasAnyRole(T(us.hogu.model.enums.UserRole).CUSTOMER.name())")
@RequiredArgsConstructor
@Tag(name = "Customer Services", description = "API trasversali per tutti i servizi dell'utente")
public class CustomerServiceController {

    private final CustomerService customerService;

    @GetMapping("/upcoming-bookings")
    @Operation(summary = "Lista prenotazioni in arrivo", description = "Prenotazioni del cliente da oggi al futuro ordinato per stato")
    public ResponseEntity<java.util.List<us.hogu.controller.dto.response.PriceChangeRequestDto>> getUpcomingBookings(
            @org.springframework.security.core.annotation.AuthenticationPrincipal us.hogu.configuration.security.dto.UserAccount userAccount,
            @org.springframework.web.bind.annotation.RequestParam(defaultValue = "0") int page,
            @org.springframework.web.bind.annotation.RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(customerService.getUpcomingBookings(userAccount.getAccountId(), page, size));
    }

    @GetMapping("/past-bookings")
    @Operation(summary = "Lista prenotazioni passate", description = "Prenotazioni del cliente fino a ieri ordinato per stato")
    public ResponseEntity<java.util.List<us.hogu.controller.dto.response.PriceChangeRequestDto>> getPastBookings(
            @org.springframework.security.core.annotation.AuthenticationPrincipal us.hogu.configuration.security.dto.UserAccount userAccount,
            @org.springframework.web.bind.annotation.RequestParam(defaultValue = "0") int page,
            @org.springframework.web.bind.annotation.RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(customerService.getPastBookings(userAccount.getAccountId(), page, size));
    }

}