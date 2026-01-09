package us.hogu.model;

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
import javax.validation.constraints.NotNull;

import org.hibernate.annotations.CreationTimestamp;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import us.hogu.model.converter.StringListConverter;

@Entity
@Table(name = "bnb_services")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BnbServiceEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull(message = "Il provider è obbligatorio")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "provider_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false)
    private Double defaultPricePerNight; // Prezzo base fallback

    @Column(nullable = false)
    private Integer totalRooms;

    @Column(nullable = false)
    private Integer maxGuestsForRoom;

    @OneToMany(mappedBy = "bnbService", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    private List<BnbRoom> rooms;

    @OneToMany(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    @JoinColumn(name = "bnb_service_id", referencedColumnName = "id")
    private List<ServiceLocale> locales;

	@Convert(converter = StringListConverter.class)
	@Column(name = "images", columnDefinition = "TEXT")
    private List<String> images;

    @CreationTimestamp
    private OffsetDateTime creationDate;

    private Boolean publicationStatus;

    @OneToMany(mappedBy = "bnbService", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    @ToString.Exclude
    private List<BnbBooking> bookings;

    @OneToMany(mappedBy = "bnbService", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    @ToString.Exclude
    private List<Review> reviews;
}
