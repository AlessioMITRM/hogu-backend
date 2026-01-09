package us.hogu.model;

import java.time.OffsetDateTime;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.Builder; // Importante per @Builder.Default
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "luggage_bookings")
public class LuggageBooking extends Booking {
    
    @Column(name = "drop_off_time")
    private OffsetDateTime dropOffTime;
    
    @Column(name = "pick_up_time")
    private OffsetDateTime pickUpTime;
    
    // --- NUOVI CAMPI BAGAGLI ---
    
    @Builder.Default
    @Column(name = "bags_small", nullable = false)
    private Integer bagsSmall = 0;

    @Builder.Default
    @Column(name = "bags_medium", nullable = false)
    private Integer bagsMedium = 0;

    @Builder.Default
    @Column(name = "bags_large", nullable = false)
    private Integer bagsLarge = 0;
    
    // Rimosso numberOfPeople
    
    private String specialRequests;
    
    @ManyToOne
    @JoinColumn(name = "luggage_service_id")
    private LuggageServiceEntity luggageService;
}