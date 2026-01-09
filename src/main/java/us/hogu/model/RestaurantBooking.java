package us.hogu.model;

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
@Table(name = "restaurant_bookings")
public class RestaurantBooking extends Booking {
    private OffsetDateTime reservationTime;
    
    private Integer numberOfPeople;
    
    private String specialRequests;
    
    @ManyToOne
    @JoinColumn(name = "restaurant_service_id", referencedColumnName = "id")
    private RestaurantServiceEntity restaurantService;
}
