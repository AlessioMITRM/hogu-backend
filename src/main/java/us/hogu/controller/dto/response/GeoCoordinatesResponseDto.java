package us.hogu.controller.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class GeoCoordinatesResponseDto {
    private double latitude;
    private double longitude;
    private String fullAddress; // L'indirizzo normalizzato trovato da Stadia
}
