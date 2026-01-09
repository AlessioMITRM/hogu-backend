package us.hogu.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
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
import lombok.ToString;
import us.hogu.model.converter.StringListConverter;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "club_services")
public class ClubServiceEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull(message = "Il provider è obbligatorio")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "provider_id", nullable = false)
    private User user;

    @NotBlank(message = "Il nome è obbligatorio")
    @Size(min = 2, max = 100, message = "Il nome deve essere tra 2 e 100 caratteri")
    @Column(nullable = false, length = 100)
    private String name;

    @NotBlank(message = "La descrizione è obbligatoria")
    @Size(min = 10, max = 1000, message = "La descrizione deve essere tra 10 e 1000 caratteri")
    @Column(nullable = false, length = 1000)
    private String description;

    @OneToMany(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
	@JoinColumn(name = "club_service_id", referencedColumnName = "id")
    private List<ServiceLocale> locales;

    @NotNull(message = "La capacità è obbligatoria")
    @Positive(message = "La capacità deve essere un numero positivo")
    @Column(nullable = false)
    private Long maxCapacity;

    @NotNull(message = "Il prezzo base è obbligatorio")
    @PositiveOrZero(message = "Il prezzo base deve essere positivo o zero")
    @Column(nullable = false)
    private BigDecimal basePrice;

	@Convert(converter = StringListConverter.class)
	@Column(name = "images", columnDefinition = "TEXT")
    private List<String> images;

    @NotNull(message = "Lo stato di pubblicazione è obbligatorio")
    @Column(nullable = false)
    private Boolean publicationStatus;
    
    @Column(nullable = false, updatable = false)
    @CreationTimestamp
    private OffsetDateTime creationDate;

    // RELAZIONE CON EVENTI
    @OneToMany(mappedBy = "clubService", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @ToString.Exclude
    private List<EventClubServiceEntity> events;

    @OneToMany(mappedBy = "clubService")
    @ToString.Exclude
    private List<ClubBooking> bookings;

    @OneToMany(mappedBy = "clubService")
    @ToString.Exclude
    private List<Review> reviews;
}
