package us.hogu.model;

import java.time.LocalDate;
import java.time.LocalDateTime;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Positive;
import javax.validation.constraints.PositiveOrZero;
import javax.validation.constraints.FutureOrPresent;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import us.hogu.model.enums.ServiceType;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "availability_slots")
public class AvailabilitySlot {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @NotNull(message = "Il tipo di servizio è obbligatorio")
    @Enumerated(EnumType.ORDINAL)
    @Column(nullable = false)
    private ServiceType serviceType;
    
    @NotNull(message = "L'ID del servizio è obbligatorio")
    @Positive(message = "L'ID del servizio deve essere positivo")
    private Long serviceId;
    
    @NotNull(message = "L'orario di inizio è obbligatorio")
    @FutureOrPresent(message = "L'orario di inizio deve essere presente o futuro")
    private LocalDateTime startTime;
    
    @NotNull(message = "L'orario di fine è obbligatorio")
    @FutureOrPresent(message = "L'orario di fine deve essere presente o futuro")
    private LocalDateTime endTime;
    
    @NotNull(message = "La capacità massima è obbligatoria")
    @Positive(message = "La capacità massima deve essere positiva")
    private Integer maxCapacity;
    
    @NotNull(message = "I posti disponibili sono obbligatori")
    @PositiveOrZero(message = "I posti disponibili devono essere positivi o zero")
    private Integer availableSlots;
    
    @NotNull(message = "La data è obbligatoria")
    @FutureOrPresent(message = "La data deve essere presente o futuro")
    private LocalDate date;
}