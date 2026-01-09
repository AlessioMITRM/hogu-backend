package us.hogu.model;

import java.time.OffsetDateTime;

import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Positive;
import javax.validation.constraints.Size;
import javax.validation.constraints.Future;
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
    @Future(message = "L'orario della prenotazione deve essere nel futuro")
    private OffsetDateTime reservationTime;
    
    @NotNull(message = "Il numero di persone è obbligatorio")
    @Positive(message = "Il numero di persone deve essere positivo")
    private Integer numberOfPeople;
    
    @Size(max = 500, message = "Le richieste speciali non possono superare 500 caratteri")
    private String specialRequests;
    
    @Column(name = "table_requested")
    private Boolean table;
    
    @NotNull(message = "Il servizio club è obbligatorio")
    @ManyToOne
    @JoinColumn(name = "club_service_id", nullable = false)
    private ClubServiceEntity clubService;
    
    @NotNull(message = "L'evento del club è obbligatorio")
    @ManyToOne
    @JoinColumn(name = "event_club_service_id", nullable = false)
    private EventClubServiceEntity eventClubService;
}