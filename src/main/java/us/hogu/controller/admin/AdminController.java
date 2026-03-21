package us.hogu.controller.admin;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
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
import us.hogu.controller.dto.request.AdminBookingStatusUpdateRequestDto;
import us.hogu.controller.dto.response.AdminBookingResponseDto;
import us.hogu.controller.dto.response.AdminCustomerDetailResponseDto;
import us.hogu.controller.dto.response.AdminCustomerResponseDto;
import us.hogu.controller.dto.response.AdminDashboardKpiResponseDto;
import us.hogu.controller.dto.response.AdminProviderResponseDto;
import us.hogu.controller.dto.response.PendingVerificationResponseDto;
import us.hogu.controller.dto.response.UserDocumentResponseDto;
import us.hogu.model.enums.BookingStatus;
import us.hogu.model.enums.UserStatus;
import us.hogu.service.intefaces.AdminService;

import javax.validation.Valid;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasAnyRole(T(us.hogu.model.enums.UserRole).ADMIN.name())")
@Tag(name = "AdminController", description = "APIs per l'admins")
public class AdminController {
	private final AdminService adminService;

	@GetMapping("/verifications/pending")
	@Operation(summary = "Lista Verifiche Servizi in Attesa", description = "Lista delle richieste di verifica dei servizi in attesa")
	@ApiResponses(value = { @ApiResponse(responseCode = "200", description = "Lista recuperata con successo") })
	public ResponseEntity<List<PendingVerificationResponseDto>> getPendingVerifications() {
		List<PendingVerificationResponseDto> response = adminService.getPendingVerifications();

		return ResponseEntity.ok(response);
	}

	@Operation(summary = "Approva una verifica servizio", description = "Consente all'amministratore di approvare una verifica di servizio")
	@ApiResponses(value = { @ApiResponse(responseCode = "200", description = "Verifica approvata con successo"),
			@ApiResponse(responseCode = "404", description = "Verifica non trovata") })
	@PostMapping("/verifications/{id}/approve")
	public ResponseEntity<String> approveServiceVerification(@PathVariable("id") Long verificationId) {
		adminService.approveServiceVerification(verificationId);

		return ResponseEntity.ok("Verifica approvata con successo");
	}

	@Operation(summary = "Recupera un documento utente", description = "Permette di scaricare il documento caricato dall'utente fornendo l'ID")
	@ApiResponses(value = { @ApiResponse(responseCode = "200", description = "Documento trovato e restituito"),
			@ApiResponse(responseCode = "404", description = "Documento non trovato") })
	@GetMapping("/verifications/documents/{id}")
	public ResponseEntity<UserDocumentResponseDto> getVerificationDocument(@PathVariable("id") Long idUserDocument) {
		return ResponseEntity.status(HttpStatus.CREATED).body(adminService.getFileUserDocument(idUserDocument));
	}

	@Operation(summary = "Rifiuta una verifica servizio", description = "Consente all'amministratore di rifiutare una verifica specificando una motivazione")
	@ApiResponses(value = { @ApiResponse(responseCode = "200", description = "Verifica rifiutata"),
			@ApiResponse(responseCode = "404", description = "Verifica non trovata") })
	@PostMapping("/verifications/{id}/reject")
	public ResponseEntity<String> rejectServiceVerification(
			@PathVariable("id") Long verificationId,
			@RequestParam String motivation) {
		adminService.rejectServiceVerification(verificationId, motivation);

		return ResponseEntity.ok("Verifica rifiutata");
	}

	@GetMapping("/customers")
	@Operation(summary = "Lista Clienti", description = "Lista paginata dei clienti con ricerca per nome e cognome")
	public ResponseEntity<Page<AdminCustomerResponseDto>> getCustomers(
			@RequestParam(required = false, defaultValue = "") String search,
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "10") int size) {
		Page<AdminCustomerResponseDto> response = adminService.getCustomers(search, page, size);
		return ResponseEntity.ok(response);
	}

	@GetMapping("/customers/{id}")
	@Operation(summary = "Dettaglio Cliente", description = "Recupera i dettagli completi di un singolo cliente per ID")
	public ResponseEntity<AdminCustomerDetailResponseDto> getCustomerDetail(@PathVariable("id") Long userId) {
		return ResponseEntity.ok(adminService.getCustomerDetail(userId));
	}

	@GetMapping("/providers")
	@Operation(summary = "Lista Provider", description = "Lista paginata dei provider con ricerca per nome")
	public ResponseEntity<Page<AdminProviderResponseDto>> getProviders(
			@RequestParam(required = false, defaultValue = "") String search,
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "10") int size) {
		Page<AdminProviderResponseDto> response = adminService.getProviders(search, page, size);
		return ResponseEntity.ok(response);
	}

	@GetMapping("/providers/{id}/documents")
	@Operation(summary = "Lista Documenti Provider", description = "Recupera l'elenco dei documenti (metadata) associati a un provider")
	public ResponseEntity<List<UserDocumentResponseDto>> getProviderDocuments(@PathVariable("id") Long providerId) {
		return ResponseEntity.ok(adminService.getProviderDocuments(providerId));
	}

	@GetMapping("/bookings")
	@Operation(summary = "Lista Prenotazioni", description = "Lista paginata di tutte le prenotazioni con ricerca per bookingCode")
	public ResponseEntity<Page<AdminBookingResponseDto>> getBookings(
			@RequestParam(required = false, defaultValue = "") String search,
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "10") int size) {
		Page<AdminBookingResponseDto> response = adminService.getBookings(search, page, size);
		return ResponseEntity.ok(response);
	}

	@GetMapping("/bookings/{id}")
	@Operation(summary = "Dettaglio Prenotazione", description = "Recupera i dettagli completi di una singola prenotazione per ID")
	public ResponseEntity<us.hogu.controller.dto.response.AdminBookingDetailResponseDto> getBookingDetail(
			@PathVariable("id") Long bookingId) {
		return ResponseEntity.ok(adminService.getBookingDetail(bookingId));
	}

	@PutMapping("/bookings/{id}/status")
	@Operation(summary = "Aggiorna stato prenotazione", description = "Permette all'admin di cambiare lo stato di una prenotazione con una motivazione")
	public ResponseEntity<String> updateBookingStatus(
			@PathVariable("id") Long bookingId,
			@Valid @RequestBody AdminBookingStatusUpdateRequestDto request) {
		adminService.updateBookingStatus(bookingId, request.getStatus(), request.getStatusReason());
		return ResponseEntity.ok("Stato prenotazione aggiornato con successo");
	}

	@PutMapping("/users/{id}/status")
	@Operation(summary = "Aggiorna stato utente", description = "Permette all'admin di cambiare lo stato di un utente (Attivo, Sospeso, Disattivato)")
	public ResponseEntity<String> updateUserStatus(
			@PathVariable("id") Long userId,
			@RequestParam UserStatus status) {
		adminService.updateUserStatus(userId, status);
		return ResponseEntity.ok("Stato utente aggiornato con successo");
	}

	/*
	 * TODO: implementare dashboard statistics and recent activities
	 */
	/*
	 * @Operation(summary = "Approva un servizio del Provider", description =
	 * "Consente all'amministratore di approvare un servizio del Provider in attesa di verifica"
	 * )
	 * 
	 * @ApiResponses(value = { @ApiResponse(responseCode = "200", description =
	 * "Account approvato con successo"),
	 * 
	 * @ApiResponse(responseCode = "404", description = "Account non trovato"),
	 * 
	 * @ApiResponse(responseCode = "400", description = "Richiesta non valida") })
	 * 
	 * @PostMapping("/provider-accounts/{id}/approve")
	 * public ResponseEntity<String> approveServiceProvider(@PathVariable("id") Long
	 * idUser)
	 * {
	 * adminService.approveProviderAccount(idUser);
	 * 
	 * return ResponseEntity.ok("Provider approvato con successo");
	 * }
	 */

	@GetMapping("/kpis")
	@Operation(summary = "Ottieni i KPI della dashboard", description = "Restituisce i dati per le card della dashboard admin")
	public ResponseEntity<AdminDashboardKpiResponseDto> getDashboardKpis() {
		return ResponseEntity.ok(adminService.getDashboardKpis());
	}
}
