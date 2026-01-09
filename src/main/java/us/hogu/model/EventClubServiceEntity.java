package us.hogu.model;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.validation.constraints.Future;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Positive;
import javax.validation.constraints.PositiveOrZero;
import javax.validation.constraints.Size;

import org.hibernate.annotations.CreationTimestamp;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import us.hogu.model.converter.StringListConverter;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "event_club_services")
public class EventClubServiceEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull(message = "Il club service è obbligatorio")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "club_service_id", nullable = false)
    private ClubServiceEntity clubService;

    @NotBlank(message = "Il nome dell'evento è obbligatorio")
    @Size(min = 2, max = 100, message = "Il nome deve essere tra 2 e 100 caratteri")
    @Column(nullable = false, length = 100)
    private String name;

    @NotBlank(message = "La descrizione dell'evento è obbligatoria")
    @Size(min = 10, max = 500, message = "La descrizione deve essere tra 10 e 500 caratteri")
    @Column(nullable = false, length = 500)
    private String description;

    @OneToMany(fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "event_id", referencedColumnName = "id")
    private List<ServiceLocale> locales = new ArrayList<>();

    @OneToMany(fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true, mappedBy = "eventClubService")
    private List<EventPricingConfiguration> pricingConfigurations = new ArrayList<>();

    @NotNull(message = "L'orario di inizio è obbligatorio")
    @Future(message = "L'orario di inizio deve essere nel futuro")
    @Column(nullable = false)
    private OffsetDateTime startTime;

    @NotNull(message = "L'orario di fine è obbligatorio")
    @Future(message = "L'orario di fine deve essere nel futuro")
    @Column(nullable = false)
    private OffsetDateTime endTime;

    @NotNull(message = "Il prezzo è obbligatorio")
    @PositiveOrZero(message = "Il prezzo deve essere positivo o zero")
    @Column(nullable = false)
    private BigDecimal price;

    @NotNull(message = "Il numero massimo di partecipanti è obbligatorio")
    @Positive(message = "Il numero massimo di partecipanti deve essere positivo")
    @Column(nullable = false)
    private Long maxCapacity;

    @NotNull(message = "Il numero di posti occupati è obbligatorio")
    @PositiveOrZero
    @Column(nullable = false)
    private Long occupiedCapacity;
    
    @Size(max = 100, message = "Il nome del DJ non può superare 100 caratteri")
    @Column(length = 100)
    private String djName;

    @Size(max = 100, message = "Il dress code non può superare 100 caratteri")
    @Column(length = 100)
    private String dressCode;

    @Column
    private Integer genderPercentage; // Percentuale maschi (0-100), opzionale

    @Size(max = 100, message = "Il tema non può superare 100 caratteri")
    @Column(length = 100)
    private String theme;

    @Convert(converter = StringListConverter.class)
    @Column(name = "images", columnDefinition = "TEXT")
    private List<String> images = new ArrayList<>();

    @NotNull(message = "Lo stato attivo è obbligatorio")
    @Column(nullable = false)
    private Boolean isActive;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private OffsetDateTime creationDate;
}
