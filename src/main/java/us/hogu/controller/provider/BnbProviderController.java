package us.hogu.controller.provider;

import java.io.IOException;
import java.util.Date;
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
import org.springframework.web.bind.annotation.DeleteMapping;
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
import us.hogu.common.constants.SuccessConstants;
import us.hogu.configuration.security.dto.UserAccount;
import us.hogu.controller.dto.request.BnbRoomPriceRequestDto;
import us.hogu.controller.dto.request.BnbRoomRequestDto;
import us.hogu.controller.dto.request.BnbServiceRequestDto;
import us.hogu.controller.dto.response.BnbBookingResponseDto;
import us.hogu.controller.dto.response.BnbRoomResponseDto;
import us.hogu.controller.dto.response.BnbServiceDetailResponseDto;
import us.hogu.controller.dto.response.BnbServiceResponseDto;
import us.hogu.controller.dto.response.ClubBookingResponseDto;
import us.hogu.controller.dto.response.InfoStatsDto;
import us.hogu.controller.dto.response.OperationResponseDto;
import us.hogu.service.intefaces.BnbService;
import us.hogu.model.enums.ServiceType;
import us.hogu.service.intefaces.PaymentService;

@RestController
@RequestMapping("/api/provider/services/bnb")
@RequiredArgsConstructor
@Tag(name = "Bnb Services Provider", description = "APIs per gestione servizi BNB e prenotazioni per Fornitore")
@PreAuthorize("hasAnyRole(T(us.hogu.model.enums.UserRole).PROVIDER.name())")
public class BnbProviderController {
	private final BnbService bnbService;
	private final PaymentService paymentService;

	@GetMapping("/get-info")
	@Operation(summary = "Statistiche deposito bagagli", description = "Restituisce le statistiche del deposito bagagli del provider autenticato")
	public ResponseEntity<InfoStatsDto> getInfo(
			@Parameter(hidden = true) @AuthenticationPrincipal UserAccount userAccount) {
		return ResponseEntity.ok(bnbService.getInfo(userAccount.getAccountId()));
	}

	@GetMapping("/{id}")
	@Operation(summary = "Dettagli B&B per modifica", description = "Recupera tutti i dati di un B&B per la pagina di modifica (solo proprietario)")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "Dettagli recuperati con successo"),
			@ApiResponse(responseCode = "404", description = "B&B non trovato") })
	public ResponseEntity<BnbServiceDetailResponseDto> getBnbServiceForEdit(
			@Parameter(hidden = true) @AuthenticationPrincipal UserAccount userAccount,
			@PathVariable Long id) {
		return ResponseEntity.ok(bnbService.getBnbServiceByIdAndProvider(id, userAccount.getAccountId()));
	}

	@Operation(summary = "Ottiene tutti i B&B pubblicati (paginato)", description = "Restituisce la lista paginata dei servizi B&B pubblicati per il provider.")
	@ApiResponse(responseCode = "200", description = "Lista servizi ottenuta con successo")
	@GetMapping("/published")
	public ResponseEntity<Page<BnbServiceResponseDto>> getAllBnbServicesByProvider(
			@Parameter(hidden = true) @AuthenticationPrincipal UserAccount userAccount,
			@Parameter(description = "Numero di pagina (0-based)") @RequestParam(defaultValue = "0") int page,
			@Parameter(description = "Dimensione della pagina") @RequestParam(defaultValue = "10") int size,
			@Parameter(description = "Campo di ordinamento") @RequestParam(defaultValue = "creationDate") String sortBy,
			@Parameter(description = "Direzione ordinamento (asc|desc)") @RequestParam(defaultValue = "desc") String direction) {
		Pageable pageable = PageRequest.of(page, size,
				direction.equalsIgnoreCase("desc") ? Sort.by(sortBy).descending() : Sort.by(sortBy).ascending());

		Page<BnbServiceResponseDto> response = bnbService.getAllBnbServicesByProvider(userAccount.getAccountId(),
				pageable);
		return ResponseEntity.ok(response);
	}

	@Operation(summary = "Lista camere del B&B del provider (paginato)", description = "Restituisce le camere associate al servizio B&B del provider autenticato.")
	@ApiResponse(responseCode = "200", description = "Camere ottenute con successo")
	@GetMapping("/rooms")
	public ResponseEntity<Page<BnbRoomResponseDto>> getRoomsForService(
			@Parameter(hidden = true) @AuthenticationPrincipal UserAccount userAccount,
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "10") int size,
			@RequestParam(defaultValue = "id") String sortBy,
			@RequestParam(defaultValue = "desc") String direction) {
		Pageable pageable = PageRequest.of(page, size,
				direction.equalsIgnoreCase("desc") ? Sort.by(sortBy).descending() : Sort.by(sortBy).ascending());

		Page<BnbRoomResponseDto> response = bnbService.getRoomsForServiceByProvider(userAccount.getAccountId(),
				pageable);
		return ResponseEntity.ok(response);
	}

	@GetMapping("/{id}/bookings")
	@Operation(summary = "Prenotazioni B&B (paginato)", description = "Restituisce le prenotazioni ricevute per un B&B (solo proprietario) con paginazione.")
	@ApiResponses(value = { @ApiResponse(responseCode = "200", description = "Prenotazioni recuperate con successo"),
			@ApiResponse(responseCode = "401", description = "Utente non autenticato o non autorizzato"),
			@ApiResponse(responseCode = "404", description = "B&B non trovato") })
	public ResponseEntity<Page<BnbBookingResponseDto>> getBnbBookings(
			@Parameter(hidden = true) @AuthenticationPrincipal UserAccount userAccount,
			@PathVariable Long id,
			@Parameter(description = "Numero di pagina (0-based)") @RequestParam(defaultValue = "0") int page,
			@Parameter(description = "Dimensione della pagina") @RequestParam(defaultValue = "10") int size,
			@Parameter(description = "Campo di ordinamento") @RequestParam(defaultValue = "creationDate") String sortBy,
			@Parameter(description = "Direzione ordinamento (asc|desc)") @RequestParam(defaultValue = "asc") String direction) {
		Pageable pageable = PageRequest.of(page, size,
				direction.equalsIgnoreCase("desc") ? Sort.by(sortBy).descending() : Sort.by(sortBy).ascending());

		Page<BnbBookingResponseDto> response = bnbService.getBookingsForProvider(userAccount, id, pageable);
		return ResponseEntity.ok(response);
	}

	@GetMapping("/{id}/bookings-today")
	@Operation(summary = "Agenda prenotazioni B&B di oggi", description = "Restituisce le prenotazioni con check-in in data odierna per il B&B (solo proprietario), ordinate dalla più recente alla più vecchia.")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "Prenotazioni recuperate con successo"),
			@ApiResponse(responseCode = "401", description = "Utente non autenticato o non autorizzato"),
			@ApiResponse(responseCode = "404", description = "B&B non trovato")
	})
	public ResponseEntity<Page<BnbBookingResponseDto>> getTodayBnbBookings(
			@Parameter(hidden = true) @AuthenticationPrincipal UserAccount userAccount,
			@PathVariable Long id,
			@Parameter(description = "Numero di pagina (0-based)") @RequestParam(defaultValue = "0") int page,
			@Parameter(description = "Dimensione della pagina") @RequestParam(defaultValue = "10") int size) {
		Pageable pageable = PageRequest.of(page, size, Sort.by("creationDate").descending());
		Page<BnbBookingResponseDto> response = bnbService.getTodayBookingsForProvider(userAccount, id, pageable);
		return ResponseEntity.ok(response);
	}

	@GetMapping("/{id}/bookings-upcoming")
	@Operation(summary = "Prenotazioni B&B da oggi in avanti", description = "Restituisce le prenotazioni con check-in da oggi in avanti per il B&B (solo proprietario), ordinate per data di check-in crescente.")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "Prenotazioni recuperate con successo"),
			@ApiResponse(responseCode = "401", description = "Utente non autenticato o non autorizzato"),
			@ApiResponse(responseCode = "404", description = "B&B non trovato")
	})
	public ResponseEntity<Page<BnbBookingResponseDto>> getUpcomingBnbBookings(
			@Parameter(hidden = true) @AuthenticationPrincipal UserAccount userAccount,
			@PathVariable Long id,
			@Parameter(description = "Numero di pagina (0-based)") @RequestParam(defaultValue = "0") int page,
			@Parameter(description = "Dimensione della pagina") @RequestParam(defaultValue = "10") int size) {
		Pageable pageable = PageRequest.of(page, size,
				Sort.by(Sort.Order.asc("checkInDate"), Sort.Order.asc("creationDate")));
		Page<BnbBookingResponseDto> response = bnbService.getUpcomingBookingsForProvider(userAccount, id, pageable);
		return ResponseEntity.ok(response);
	}

	@GetMapping("/{id}/bookings-history")
	@Operation(summary = "Storico prenotazioni B&B (paginato)", description = "Restituisce lo STORICO delle prenotazioni passate (fino a ieri) per il B&B (solo proprietario), ordinate per data di check-in decrescente.")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "Prenotazioni recuperate con successo"),
			@ApiResponse(responseCode = "401", description = "Utente non autenticato o non autorizzato"),
			@ApiResponse(responseCode = "404", description = "B&B non trovato")
	})
	public ResponseEntity<Page<BnbBookingResponseDto>> getHistoryBnbBookings(
			@Parameter(hidden = true) @AuthenticationPrincipal UserAccount userAccount,
			@PathVariable Long id,
			@Parameter(description = "Numero di pagina (0-based)") @RequestParam(defaultValue = "0") int page,
			@Parameter(description = "Dimensione della pagina") @RequestParam(defaultValue = "10") int size) {
		Pageable pageable = PageRequest.of(page, size,
				Sort.by(Sort.Order.desc("checkInDate"), Sort.Order.desc("creationDate")));
		Page<BnbBookingResponseDto> response = bnbService.getHistoryBookingsForProvider(userAccount, id, pageable);
		return ResponseEntity.ok(response);
	}

	@GetMapping("/booking/validate")
	@Operation(summary = "Valida codice prenotazione B&B", description = "Verifica se il codice prenotazione è valido e appartiene al provider")
	public ResponseEntity<us.hogu.controller.dto.response.BnbBookingValidationResponseDto> validateBookingCode(
			@Parameter(hidden = true) @AuthenticationPrincipal UserAccount userAccount,
			@RequestParam String code) {
		us.hogu.controller.dto.response.BnbBookingValidationResponseDto response = bnbService
				.validateBnbBookingByCode(userAccount.getAccountId(), code);
		return ResponseEntity.ok(response);
	}

	@PutMapping("/booking/{bookingId}/reject")
	@Operation(summary = "Rifiuta prenotazione B&B", description = "Rifiuta una prenotazione B&B, annulla l'eventuale pre-autorizzazione PayPal e imposta lo stato a CANCELLED_BY_PROVIDER")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "204", description = "Prenotazione rifiutata con successo"),
			@ApiResponse(responseCode = "404", description = "Prenotazione non trovata o non autorizzata")
	})
	public ResponseEntity<Void> rejectBooking(
			@Parameter(hidden = true) @AuthenticationPrincipal UserAccount userAccount,
			@Parameter(description = "ID della prenotazione") @PathVariable Long bookingId,
			@Parameter(description = "Motivazione rifiuto") @RequestParam(required = false) String reason) {
		paymentService.cancelBookingByProvider(bookingId, ServiceType.BNB, userAccount.getAccountId(), reason);
		return ResponseEntity.noContent().build();
	}

	@DeleteMapping("/booking/{bookingId}")
	@Operation(summary = "Cancella/Rifiuta prenotazione B&B", description = "Cancella o rifiuta una prenotazione B&B e gestisce l'annullamento/rimborso del pagamento, impostando lo stato a CANCELLED_BY_PROVIDER")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "204", description = "Prenotazione cancellata/rifiutata con successo"),
			@ApiResponse(responseCode = "404", description = "Prenotazione non trovata o non autorizzata")
	})
	public ResponseEntity<Void> deleteBooking(
			@Parameter(hidden = true) @AuthenticationPrincipal UserAccount userAccount,
			@Parameter(description = "ID della prenotazione") @PathVariable Long bookingId,
			@Parameter(description = "Motivazione cancellazione") @RequestParam(required = false) String reason) {
		paymentService.cancelBookingByProvider(bookingId, ServiceType.BNB, userAccount.getAccountId(), reason);
		return ResponseEntity.noContent().build();
	}

	@PutMapping("/booking/{bookingId}/cancel")
	@Operation(summary = "Cancella prenotazione B&B", description = "Cancella una prenotazione B&B e gestisce l'annullamento/rimborso del pagamento, impostando lo stato a CANCELLED_BY_PROVIDER")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "204", description = "Prenotazione cancellata con successo"),
			@ApiResponse(responseCode = "404", description = "Prenotazione non trovata o non autorizzata")
	})
	public ResponseEntity<Void> cancelBooking(
			@Parameter(hidden = true) @AuthenticationPrincipal UserAccount userAccount,
			@Parameter(description = "ID della prenotazione") @PathVariable Long bookingId,
			@Parameter(description = "Motivazione cancellazione") @RequestParam(required = false) String reason) {
		paymentService.cancelBookingByProvider(bookingId, ServiceType.BNB, userAccount.getAccountId(), reason);
		return ResponseEntity.noContent().build();
	}

	@PutMapping("/booking/{bookingId}/accept")
	@Operation(summary = "Accetta prenotazione B&B", description = "Accetta una prenotazione B&B e cattura il pagamento PayPal bloccato (se applicabile)")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "204", description = "Prenotazione accettata con successo"),
			@ApiResponse(responseCode = "404", description = "Prenotazione non trovata o non autorizzata"),
			@ApiResponse(responseCode = "400", description = "Errore nel pagamento o stato non valido")
	})
	public ResponseEntity<Void> acceptBooking(
			@Parameter(hidden = true) @AuthenticationPrincipal UserAccount userAccount,
			@Parameter(description = "ID della prenotazione") @PathVariable Long bookingId) {
		paymentService.confirmBookingByProvider(bookingId, ServiceType.BNB, userAccount.getAccountId());
		return ResponseEntity.noContent().build();
	}

	@PostMapping("/booking/{bookingId}/confirm")
	@Operation(summary = "Accetta prenotazione B&B (Confirm)", description = "Accetta una prenotazione B&B e cattura il pagamento PayPal bloccato (se applicabile)")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "204", description = "Prenotazione accettata con successo"),
			@ApiResponse(responseCode = "404", description = "Prenotazione non trovata o non autorizzata"),
			@ApiResponse(responseCode = "400", description = "Errore nel pagamento o stato non valido")
	})
	public ResponseEntity<Void> confirmBooking(
			@Parameter(hidden = true) @AuthenticationPrincipal UserAccount userAccount,
			@Parameter(description = "ID della prenotazione") @PathVariable Long bookingId) {
		paymentService.confirmBookingByProvider(bookingId, ServiceType.BNB, userAccount.getAccountId());
		return ResponseEntity.noContent().build();
	}

	@GetMapping("/rooms/{id}")
	@Operation(summary = "Dettaglio camera B&B per provider", description = "Restituisce le informazioni di una camera B&B appartenente al provider autenticato.")
	@ApiResponses({ @ApiResponse(responseCode = "200", description = "Camera trovata"),
			@ApiResponse(responseCode = "404", description = "Camera non trovata") })
	public ResponseEntity<BnbRoomResponseDto> getRoomByIdForProvider(
			@Parameter(hidden = true) @AuthenticationPrincipal UserAccount userAccount, @PathVariable Long id) {
		BnbRoomResponseDto response = bnbService.getRoomByIdForProvider(id, userAccount.getAccountId());
		return ResponseEntity.ok(response);
	}

	// 🔹 CREA UN NUOVO SERVIZIO B&B
	@PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	@Operation(summary = "Crea un nuovo servizio B&B", description = "Permette di creare un nuovo servizio B&B associato a un provider.")
	@ApiResponses({ @ApiResponse(responseCode = "200", description = "Servizio creato con successo"),
			@ApiResponse(responseCode = "404", description = "Fornitore non trovato") })
	public ResponseEntity<BnbServiceResponseDto> createBnbService(
			@Parameter(hidden = true) @AuthenticationPrincipal UserAccount userAccount,
			@RequestPart("data") @Valid BnbServiceRequestDto request,
			@RequestPart(value = "images", required = false) List<MultipartFile> images) throws IOException {
		return ResponseEntity.ok(bnbService.createBnbService(userAccount, request, images));
	}

	// 🔹 AGGIORNA UN SERVIZIO B&B
	@PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	@Operation(summary = "Aggiorna un servizio B&B", description = "Permette di aggiornare i dettagli di un servizio B&B esistente.")
	@ApiResponses({ @ApiResponse(responseCode = "200", description = "Servizio aggiornato con successo"),
			@ApiResponse(responseCode = "404", description = "Servizio non trovato") })
	public ResponseEntity<BnbServiceDetailResponseDto> updateBnbService(
			@Parameter(hidden = true) @AuthenticationPrincipal UserAccount userAccount, @PathVariable Long id,
			@RequestPart("data") @Valid BnbServiceRequestDto request,
			@RequestPart(value = "images", required = false) List<MultipartFile> images)
			throws Exception {
		return ResponseEntity.ok(bnbService.updateBnbService(id, userAccount, request, images));
	}

	// 🔹 AGGIUNGI CAMERA A UN SERVIZIO
	@PostMapping(value = "/create-room", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	@Operation(summary = "Aggiunge una camera a un servizio B&B", description = "Permette di aggiungere una nuova camera a un B&B.")
	@ApiResponses({ @ApiResponse(responseCode = "200", description = "Camera aggiunta con successo"),
			@ApiResponse(responseCode = "404", description = "Servizio non trovato") })
	public ResponseEntity<BnbRoomResponseDto> addRoomToService(
			@Parameter(hidden = true) @AuthenticationPrincipal UserAccount userAccount,
			@RequestPart("data") @Valid BnbRoomRequestDto request,
			@RequestPart(value = "images", required = false) List<MultipartFile> images) throws Exception {
		return ResponseEntity.ok(
				bnbService.addRoomToService(userAccount, userAccount.getAccountId(), request, images));
	}

	// 🔹 AGGIORNA CAMERA DI UN SERVIZIO
	@PutMapping(value = "/{roomId}/update-room", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	@Operation(summary = "Aggiorna una camera di un servizio B&B", description = "Permette di modificare i dettagli di una camera esistente di un B&B.")
	@ApiResponses({ @ApiResponse(responseCode = "200", description = "Camera aggiornata con successo"),
			@ApiResponse(responseCode = "404", description = "Camera o servizio non trovato") })
	public ResponseEntity<Object> updateRoom(@Parameter(hidden = true) @AuthenticationPrincipal UserAccount userAccount,
			@PathVariable Long roomId,
			@RequestPart("data") @Valid BnbRoomRequestDto request,
			@RequestPart(value = "images", required = false) List<MultipartFile> images) throws Exception {
		return ResponseEntity
				.ok(bnbService.updateRoom(userAccount, userAccount.getAccountId(), roomId, request, images));
	}

	// 🔹 AGGIUNGI PERIODO DI PREZZO AD UNA CAMERA
	@PostMapping("/rooms/{roomId}/prices")
	@Operation(summary = "Aggiunge un periodo di prezzo a una camera", description = "Permette di configurare prezzi stagionali o specifici per una camera.")
	@ApiResponses({ @ApiResponse(responseCode = "200", description = "Prezzo aggiunto con successo"),
			@ApiResponse(responseCode = "404", description = "Camera non trovata") })
	public ResponseEntity<OperationResponseDto> addRoomPrice(
			@Parameter(hidden = true) @AuthenticationPrincipal UserAccount userAccount, @PathVariable Long roomId,
			@RequestBody BnbRoomPriceRequestDto dto) {
		bnbService.addRoomPrice(userAccount, roomId, dto);

		return ResponseEntity.ok(new OperationResponseDto(SuccessConstants.SUCCESS.name(),
				SuccessConstants.GENERAL_SUCCESS.getMessage(), new Date()));
	}

}
