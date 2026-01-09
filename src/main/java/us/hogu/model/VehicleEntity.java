package us.hogu.model;

import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "vehicles")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VehicleEntity {
	
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Integer numberOfSeats;

    private String plateNumber; // es. targa
    
    private String model;       // modello
    
    private String type;        // tipo veicolo
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ncc_service_id")
    private NccServiceEntity nccService; // riferimento al servizio
}

