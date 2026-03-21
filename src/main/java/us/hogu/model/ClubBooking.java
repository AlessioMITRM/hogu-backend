package us.hogu.model;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Positive;
import javax.validation.constraints.Size;
import javax.persistence.Column;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder; 

@Entity
@SuperBuilder
@Table(name = "club_bookings") 
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class ClubBooking extends Booking {
    
    @NotNull(message = "L'orario della prenotazione è obbligatorio")
    private OffsetDateTime reservationTime;
     
    @NotNull(message = "Il numero di persone  è  obbligatorio")
    @Positive(message = "Il numero di persone deve essere positivo") 
    private Integer numberOfPeople;
    
    @NotNull(message = "Il servizio club è obbligatorio")
    @ManyToOne
    @JoinColumn(name = "club_service_id", nullable = false)
    private ClubServiceEntity clubService;
    
    @NotNull(message = "L'evento del club è obbligatorio")
    @ManyToOne
    @JoinColumn(name = "event_club_service_id", nullable = false)
    private EventClubServiceEntity eventClubService;

    @Column(length = 500)
    private String specialRequests;

    @Column(length = 20)
    private String pricingType;

    @Column(length = 100)
    private String pricingDescription;

    @Column
    private BigDecimal pricingPrice;
}