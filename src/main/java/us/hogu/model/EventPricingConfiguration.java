package us.hogu.model;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Positive;
import javax.validation.constraints.PositiveOrZero;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import us.hogu.model.enums.PricingType;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "event_pricing_configurations")
public class EventPricingConfiguration {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @NotNull(message = "L'evento club service è obbligatorio")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_club_service_id", nullable = false)
    private EventClubServiceEntity eventClubService;
    
    @NotNull(message = "Il tipo di prezzo è obbligatorio")
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PricingType pricingType;
    
    @Column(length = 100)
    private String description;
    
    @NotNull(message = "Il prezzo è obbligatorio")
    @PositiveOrZero(message = "Il prezzo deve essere positivo o zero")
    @Column(nullable = false)
    private Double price;
    
    @Positive(message = "La capacità deve essere positiva")
    @Column
    private Integer capacity;
    
    @Column
    private Boolean isActive;
}