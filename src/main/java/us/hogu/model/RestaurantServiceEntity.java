package us.hogu.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

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
import us.hogu.model.internal.Menu;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "restaurant_services")
public class RestaurantServiceEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @NotNull(message = "Il provider è obbligatorio")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "provider_id", nullable = false)
    private User user;

    private String name;
    
    private String description;
    
    @OneToMany(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
	@JoinColumn(name = "restaurant_service_id")
    private List<ServiceLocale> locales;
    
    @Column(name = "menu", columnDefinition = "TEXT")
    private String menu;
    
    private Integer capacity;
    
    @Column(name = "base_price")
    private BigDecimal basePrice;
    
	@Convert(converter = StringListConverter.class)
	@Column(name = "images", columnDefinition = "TEXT")
    private List<String> images;
    
    @Column(name = "creation_date")
    @CreationTimestamp
    private OffsetDateTime creationDate;
    
    @Column(name = "publication_status")
    private Boolean publicationStatus;
    
    @OneToMany(mappedBy = "restaurantService")
    @ToString.Exclude
    private List<RestaurantBooking> bookings;
    
    @OneToMany(mappedBy = "restaurantService")
    @ToString.Exclude
    private List<Review> reviews;
}
