package us.hogu.controller.publicapi;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import us.hogu.controller.dto.request.BnbSearchRequestDto;
import us.hogu.controller.dto.response.BnbRoomResponseDto;
import us.hogu.controller.dto.response.BnbSearchResponseDto;
import us.hogu.controller.dto.response.BnbServiceResponseDto;
import us.hogu.service.intefaces.BnbService;

@RestController
@RequestMapping("/api/public/services/bnb")
@RequiredArgsConstructor
@Tag(name = "Bnb Services Public", description = "APIs pubbliche per consultare bnb")
@Validated
public class BnbPublicController {
	private final BnbService bnbService;

	@Operation(summary = "Ricerca avanzata B&B", description = "Ricerca di camere B&B con filtri avanzati")
	@ApiResponses({ @ApiResponse(responseCode = "200", description = "Ricerca completata con successo"),
			@ApiResponse(responseCode = "400", description = "Parametri di ricerca non validi") })
	@GetMapping
	public ResponseEntity<BnbSearchResponseDto> searchBnbRooms(
			@javax.validation.Valid BnbSearchRequestDto searchRequest) 
	{
		BnbSearchResponseDto response = bnbService.searchBnbRooms(searchRequest);
		return ResponseEntity.ok(response);
	}

	@Operation(summary = "Lista camere di un servizio B&B (paginato)", description = "Restituisce in modo paginato le camere associate a un determinato servizio B&B.")
	@ApiResponse(responseCode = "200", description = "Camere ottenute con successo")
	@GetMapping("/{bnbServiceId}/rooms")
	public ResponseEntity<Page<BnbRoomResponseDto>> getRoomsForService(@PathVariable Long bnbServiceId,
			@RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "10") int size,
			@RequestParam(defaultValue = "creationDate") String sortBy,
			@RequestParam(defaultValue = "desc") String direction) 
	{
		Pageable pageable = PageRequest.of(page, size,
				direction.equalsIgnoreCase("desc") ? Sort.by(sortBy).descending() : Sort.by(sortBy).ascending());

		Page<BnbRoomResponseDto> response = bnbService.getRoomsForService(bnbServiceId, pageable);
		return ResponseEntity.ok(response);
	}

	@Operation(summary = "Dettaglio servizio B&B", description = "Restituisce i dettagli di un servizio B&B per ID.")
	@ApiResponses({ @ApiResponse(responseCode = "200", description = "Servizio trovato"),
			@ApiResponse(responseCode = "404", description = "Servizio non trovato") })
	@GetMapping("/{id}")
	public ResponseEntity<BnbServiceResponseDto> getBnbServiceById(@PathVariable Long id) 
	{
		Optional<BnbServiceResponseDto> service = bnbService.getBnbServiceById(id);
		return service.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
	}

	@Operation(summary = "Dettaglio camera B&B", description = "Restituisce i dettagli di una camera B&B per ID con disponibilità, prezzo totale e locali filtrati per lingua.")
	@ApiResponses({ @ApiResponse(responseCode = "200", description = "Camera trovata"),
			@ApiResponse(responseCode = "404", description = "Camera non trovata") })
	@GetMapping("/room/{id}")
	public ResponseEntity<BnbRoomResponseDto> getRoomById(@PathVariable Long id,
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate checkIn,
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate checkOut) 
	{
		BnbRoomResponseDto response = bnbService.getRoomById(id, checkIn, checkOut);
		return ResponseEntity.ok(response);
	}
}
