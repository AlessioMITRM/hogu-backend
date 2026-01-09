package us.hogu.controller.publicapi;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import lombok.RequiredArgsConstructor;
import us.hogu.controller.dto.request.DistanceRequestDto;
import us.hogu.controller.dto.response.DistanceResponseDto;
import us.hogu.controller.dto.response.GeoCoordinatesResponseDto;
import us.hogu.service.intefaces.StadiaMapService;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/public/maps")
public class MapController {
    private final StadiaMapService stadiaMapService;

    
    @GetMapping("/geocoding")
    public ResponseEntity<GeoCoordinatesResponseDto> getGeocoding(String address) 
    {
        return ResponseEntity.ok(stadiaMapService.getCoordinatesFromAddress(address));
    }
    
    @PostMapping("/calculate-distance")
    public ResponseEntity<DistanceResponseDto> getDistance(@RequestBody DistanceRequestDto request) {
        DistanceResponseDto response = stadiaMapService.calculateDistance(
            request.getOriginLat(), request.getOriginLon(),
            request.getDestLat(), request.getDestLon()
        );
        return ResponseEntity.ok(response);
    }
}
