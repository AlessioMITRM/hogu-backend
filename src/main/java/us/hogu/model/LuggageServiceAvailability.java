package us.hogu.model;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.Builder; // Importante per @Builder.Default

@Entity
@Table(name = "luggage_service_availability")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LuggageServiceAvailability {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "luggage_service_id", nullable = false)
    private LuggageServiceEntity luggageService;

    @Column(nullable = false)
    private LocalDate date;

    @Column(nullable = false)
    private Long maxCapacity;

    @Column(nullable = false)
    private Long occupiedCapacity;
}
