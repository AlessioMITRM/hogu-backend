package us.hogu.controller.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import us.hogu.model.NccServiceEntity;

@Getter
@AllArgsConstructor
@Builder
public class VehicleEntityResponseDto {
    private Long id;

    private Integer numberOfSeats;

    private String plateNumber; // es. targa
    
    private String model;       // modello
    
    private String type;        // tipo veicolo
    
    private NccServiceEntity nccService; // riferimento al servizio
}
