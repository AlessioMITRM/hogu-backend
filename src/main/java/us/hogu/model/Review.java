package us.hogu.model;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;

import org.hibernate.annotations.CreationTimestamp;

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
@Table(name = "reviews")
public class Review {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;

    @NotNull(message = "Il tipo di servizio è obbligatorio")
    @Enumerated(EnumType.ORDINAL)
    @Column(nullable = false)
    private ServiceType serviceType;
    
    private Long serviceId;
    
    private Integer rating;
    
    private String commento;
    
    @ManyToOne
    @JoinColumn(name = "luggage_service_id")
    private LuggageServiceEntity luggageService;
    
    @ManyToOne
    @JoinColumn(name = "club_service_id")
    private ClubServiceEntity clubService;
    
    @ManyToOne
    @JoinColumn(name = "ncc_service_id")
    private NccServiceEntity nccService;
    
    @ManyToOne
    @JoinColumn(name = "bnb_service_id")
    private BnbServiceEntity bnbService;
    
    @ManyToOne
    @JoinColumn(name = "restaurant_services_id")
    private RestaurantServiceEntity restaurantService;
    
    @CreationTimestamp
    private OffsetDateTime dataCreazione;
}
