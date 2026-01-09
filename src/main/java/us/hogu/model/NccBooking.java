package us.hogu.model;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;

import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;


@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "ncc_bookings")
public class NccBooking extends Booking {
    
    private OffsetDateTime pickupTime;
    
    private String pickupLocation;
    
    private String destination;
    
    @ManyToOne
    @JoinColumn(name = "ncc_service_id")
    private NccServiceEntity nccService;
}
