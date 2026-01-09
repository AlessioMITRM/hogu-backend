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
import javax.validation.constraints.NotNull;

import org.hibernate.annotations.CreationTimestamp;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import us.hogu.model.converter.StringListConverter;

//Luggage Service

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "luggage_services")
public class LuggageServiceEntity {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

    @NotNull(message = "Il provider è obbligatorio")
    @ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "provider_id", referencedColumnName = "id")
    private User user;
	
	private String name;

	private String description;

	@OneToMany(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
	@JoinColumn(name = "luggage_service_id")
	private List<ServiceLocale> locales;
    
	private Integer capacity;

	private BigDecimal basePrice;

	@Convert(converter = StringListConverter.class)
	@Column(name = "images", columnDefinition = "TEXT")
    private List<String> images;

	private Boolean publicationStatus;
	
	@OneToMany(mappedBy = "luggageService", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
	private List<LuggageSizePrice> sizePrices;
	
	@OneToMany(mappedBy = "luggageService", cascade = CascadeType.ALL, orphanRemoval = true)
	private List<OpeningHour> openingHours;

	@OneToMany(mappedBy = "luggageService")
	@ToString.Exclude
	private List<LuggageBooking> bookings;

	@OneToMany(mappedBy = "luggageService")
	@ToString.Exclude
	private List<Review> reviews;
	
	@CreationTimestamp
	private OffsetDateTime creationDate;
}
