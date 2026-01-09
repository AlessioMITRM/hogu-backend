package us.hogu.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import us.hogu.controller.dto.response.DistanceResponseDto;
import us.hogu.controller.dto.response.GeoCoordinatesResponseDto;
import us.hogu.service.intefaces.StadiaMapService;

import java.net.URI;
import java.util.List;
import java.util.Map;

@RequiredArgsConstructor
@Service
public class StadiaMapServiceImpl implements StadiaMapService {

	@Value("${stadia.api.key}")
	private String apiKey;

	@Value("${stadia.api.base-url}")
	private String baseUrl;

	private final RestTemplate restTemplate = new RestTemplate();
	private final ObjectMapper objectMapper = new ObjectMapper();

	@Override
	public DistanceResponseDto calculateDistance(double originLat, double originLon, double destLat, double destLon) {

		// 1. URL Endpoint (senza /v1/driving, usiamo l'endpoint root Valhalla)
		String url = baseUrl + "/route?api_key=" + apiKey;

		// 2. Costruzione Body (Identica al tuo $body di PowerShell)
		Map<String, Object> requestBody = Map.of("locations",
				List.of(Map.of("lat", originLat, "lon", originLon), Map.of("lat", destLat, "lon", destLon)), "costing",
				"auto", "directions_options", Map.of("units", "km") // Chiediamo esplicitamente KM
		);

		// 3. Headers
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);
		HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

		try {
			// 4. Chiamata POST
			String jsonResponse = restTemplate.postForObject(url, entity, String.class);
			JsonNode root = objectMapper.readTree(jsonResponse);

			// 5. Navigazione nel JSON (trip -> summary)
			JsonNode trip = root.path("trip");

			// Controllo sicurezza se il trip non esiste
			if (trip.isMissingNode()) {
				throw new RuntimeException("Errore API Stadia: campo 'trip' mancante. Response: " + jsonResponse);
			}

			JsonNode summary = trip.path("summary");

			// 6. Estrazione Valori
			// length: dal tuo JSON è 574.827 -> Sono già KM (perché units=km)
			double distanceVal = summary.path("length").asDouble();

			// time: dal tuo JSON è 17413.835 -> Sono Secondi
			double timeVal = summary.path("time").asDouble();

			return DistanceResponseDto.builder().distanceKm(distanceVal) // Nessuna divisione, è già in KM
					.durationMinutes(timeVal / 60.0) // Secondi -> Minuti
					.build();

		} catch (Exception e) {
			throw new RuntimeException("Errore durante il calcolo percorso Stadia Maps: " + e.getMessage(), e);
		}
	}

	@Override
	public GeoCoordinatesResponseDto getCoordinatesFromAddress(String address) {
		// 1. PRIMO TENTATIVO: v2/autocomplete
		try {
			return callStadiaAutocomplete(address);
		} catch (Exception e) {
			System.out.println("Autocomplete fallito (" + e.getMessage() + "), provo con search...");
		}

		// 2. SECONDO TENTATIVO: v1/search
		try {
			return callStadiaSearch(address);
		} catch (Exception e) {
			System.out.println("Search fallito (" + e.getMessage() + "), provo pulizia indirizzo...");
		}

		// 3. TERZO TENTATIVO: Pulizia indirizzo (rimozione civico)
		String addressWithoutNumber = address.replaceAll("\\s\\d+([\\/\\-]\\w+)?\\b", "");
		if (!addressWithoutNumber.equals(address)) {
			try {
				return callStadiaSearch(addressWithoutNumber);
			} catch (Exception e) {
				// Fallito anche questo
			}
		}

		throw new RuntimeException("Impossibile geocodificare: " + address);
	}

	// METODO 1: Autocomplete (v2) - CORRETTO
	private GeoCoordinatesResponseDto callStadiaAutocomplete(String searchText) {
		// Costruzione pulita dell'URI
		URI uri = UriComponentsBuilder.fromHttpUrl(baseUrl + "/geocoding/v2/autocomplete")
				.queryParam("text", searchText) // NON codificare manualmente qui, ci pensa il Builder
				.queryParam("focus.point.lat", 41.9028).queryParam("focus.point.lon", 12.4964).queryParam("size", 5)
				.queryParam("api_key", apiKey).build().encode() // Codifica i parametri speciali
				.toUri();

		System.out.println("Calling Stadia Autocomplete: " + uri);

		try {
			// Passiamo l'oggetto URI, non la stringa!
			ResponseEntity<Map> response = restTemplate.getForEntity(uri, Map.class);
			Map<String, Object> body = response.getBody();

			if (body == null || !body.containsKey("features")) {
				throw new RuntimeException("Body vuoto o mancante");
			}

			List<Map<String, Object>> features = (List<Map<String, Object>>) body.get("features");

			if (features == null || features.isEmpty()) {
				throw new RuntimeException("Nessun risultato da autocomplete");
			}

			return parseStadiaResponse(features.get(0));

		} catch (HttpClientErrorException e) {
			throw new RuntimeException("Errore HTTP Autocomplete: " + e.getStatusCode());
		}
	}

	// METODO 2: Search (v1) - CORRETTO
	private GeoCoordinatesResponseDto callStadiaSearch(String searchText) {
		URI uri = UriComponentsBuilder.fromHttpUrl(baseUrl + "/geocoding/v1/search").queryParam("text", searchText)
				.queryParam("focus.point.lat", 41.9028).queryParam("focus.point.lon", 12.4964).queryParam("size", 5)
				.queryParam("api_key", apiKey).build().encode().toUri();

		System.out.println("Calling Stadia Search: " + uri);

		try {
			ResponseEntity<Map> response = restTemplate.getForEntity(uri, Map.class);
			Map<String, Object> body = response.getBody();

			if (body == null || !body.containsKey("features")) {
				throw new RuntimeException("Body vuoto o mancante");
			}

			List<Map<String, Object>> features = (List<Map<String, Object>>) body.get("features");

			if (features == null || features.isEmpty()) {
				throw new RuntimeException("Nessun risultato da search");
			}

			return parseStadiaResponse(features.get(0));

		} catch (HttpClientErrorException e) {
			throw new RuntimeException("Errore HTTP Search: " + e.getStatusCode());
		}
	}

	private GeoCoordinatesResponseDto parseStadiaResponse(Map<String, Object> feature) {
		Map<String, Object> geometry = (Map<String, Object>) feature.get("geometry");
		List<Double> coordinates = (List<Double>) geometry.get("coordinates");
		Map<String, Object> properties = (Map<String, Object>) feature.get("properties");

		String fullAddress = (String) properties.get("label");
		if (fullAddress == null) {
			fullAddress = (String) properties.get("name");
		}

		// Stadia restituisce [lon, lat], il tuo DTO probabilmente vuole lat, lon o
		// viceversa.
		// Verifico l'ordine standard GeoJSON: Indice 0 = Longitudine, Indice 1 =
		// Latitudine
		return GeoCoordinatesResponseDto.builder().latitude(coordinates.get(1)).longitude(coordinates.get(0))
				.fullAddress(fullAddress).build();
	}
}