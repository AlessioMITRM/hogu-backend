package us.hogu.controller.admin;

import java.util.List;

import javax.validation.Valid;

import org.springframework.data.repository.query.Param;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import us.hogu.controller.dto.request.CustomerRegistrationRequestDto;
import us.hogu.controller.dto.response.AuthResponseDto;
import us.hogu.controller.dto.response.PendingUserResponseDto;
import us.hogu.controller.dto.response.UserDocumentResponseDto;
import us.hogu.service.intefaces.AdminService;
import us.hogu.service.intefaces.UserService;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasAnyRole(T(us.hogu.model.enums.UserRole).ADMIN.name())")
@Tag(name = "AdminController", description = "APIs per l'admins")
public class AdminController {
	private final AdminService adminService;

	
	@GetMapping("/provider-accounts/pending")
	@Operation(summary = "Lista Account Provider in Attesa", description = "Lista account Provider in attesa")
	@ApiResponses(value = { @ApiResponse(responseCode = "201", description = "Fornitore approvato con successo"),
			@ApiResponse(responseCode = "400", description = "Dati di registrazione non validi"), })
	public ResponseEntity<List<PendingUserResponseDto>> providerAccountsPending() {
		List<PendingUserResponseDto> response = adminService.getProviderAccountsPending();

		return ResponseEntity.status(HttpStatus.CREATED).body(response);
	}

	@Operation(summary = "Approva un account Provider", description = "Consente all'amministratore di approvare un account Provider in attesa di verifica")
	@ApiResponses(value = { @ApiResponse(responseCode = "200", description = "Account approvato con successo"),
			@ApiResponse(responseCode = "404", description = "Account non trovato"),
			@ApiResponse(responseCode = "400", description = "Richiesta non valida") })
	@PostMapping("/provider-accounts/{id}/approve")
	public ResponseEntity<String> approveProviderAccount(@PathVariable("id") Long idUser) 
	{
		adminService.approveProviderAccount(idUser);
		
		return ResponseEntity.ok("Provider approvato con successo");
	}

	@Operation(summary = "Recupera il documento di un provider", description = "Permette di scaricare il documento caricato dall'utente fornendo l'ID")
	@ApiResponses(value = { @ApiResponse(responseCode = "200", description = "Documento trovato e restituito"),
			@ApiResponse(responseCode = "404", description = "Documento non trovato"),
			@ApiResponse(responseCode = "400", description = "Richiesta non valida") })
	@GetMapping("/provider-accounts/{id}/document")
	public ResponseEntity<UserDocumentResponseDto> getProviderDocument(@PathVariable("id") Long idUserDocument) 
	{
		return ResponseEntity.status(HttpStatus.CREATED).body(adminService.getFileUserDocument(idUserDocument));
	}

	@Operation(summary = "Rifiuta un account Provider", description = "Consente all'amministratore di rifiutare un account Provider specificando (opzionale) una motivazione")
	@ApiResponses(value = { @ApiResponse(responseCode = "200", description = "Account rifiutato"),
			@ApiResponse(responseCode = "404", description = "Account non trovato"),
			@ApiResponse(responseCode = "400", description = "Richiesta non valida") })
	@PostMapping("/provider-accounts/{id}/reject")
	public ResponseEntity<String> rejectProviderAccount(
			@PathVariable("id") Long idUser,
	        @RequestParam String motivation)
	{
		adminService.rejectProviderAccount(idUser, motivation);
		
		return ResponseEntity.ok("Provider rifiutato");
	}

	/*@Operation(summary = "Approva un servizio del Provider", description = "Consente all'amministratore di approvare un servizio del Provider in attesa di verifica")
	@ApiResponses(value = { @ApiResponse(responseCode = "200", description = "Account approvato con successo"),
			@ApiResponse(responseCode = "404", description = "Account non trovato"),
			@ApiResponse(responseCode = "400", description = "Richiesta non valida") })
	@PostMapping("/provider-accounts/{id}/approve")
	public ResponseEntity<String> approveServiceProvider(@PathVariable("id") Long idUser) 
	{
		adminService.approveProviderAccount(idUser);
		
		return ResponseEntity.ok("Provider approvato con successo");
	}*/
}
