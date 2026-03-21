package us.hogu.service.availability;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Set;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import us.hogu.configuration.properties.AvailabilityProperties;
import us.hogu.model.RestaurantBooking;
import us.hogu.model.RestaurantServiceEntity;
import us.hogu.model.enums.BookingStatus;
import us.hogu.repository.jpa.RestaurantBookingJpa;
import us.hogu.service.redis.RedisAvailabilityService;

@Slf4j
@Component
@RequiredArgsConstructor
public class RestaurantAvailabilityWarmupRunner implements ApplicationRunner {
    private final AvailabilityProperties availabilityProperties;
    private final RestaurantBookingJpa restaurantBookingJpa;
    private final RedisAvailabilityService redisAvailabilityService;

    @Override
    @Transactional(readOnly = true)
    public void run(ApplicationArguments args) {
        if (!availabilityProperties.getWarmup().isEnabled()) {
            return;
        }

        if (availabilityProperties.getWarmup().getFlushNamespace().isEnabled()) {
            long deletedAvailability = redisAvailabilityService.deleteByPattern("restaurant:service:*");
            // No specific warmup mark for restaurant yet, keeping it simple as per existing patterns
            log.info("Availability warmup (Restaurant): deletedAvailabilityKeys={}", deletedAvailability);
        }

        Set<BookingStatus> statuses = AvailabilityBookingStatusPolicy.occupyingStatuses();
        List<RestaurantBooking> bookings = restaurantBookingJpa.findAllByStatusInWithService(statuses);

        long applied = 0;
        for (RestaurantBooking booking : bookings) {
            if (booking == null || booking.getId() == null) {
                continue;
            }

            RestaurantServiceEntity restaurant = booking.getRestaurantService();
            if (restaurant == null || restaurant.getId() == null) {
                continue;
            }

            OffsetDateTime reservationTime = booking.getReservationTime();
            if (reservationTime == null) {
                continue;
            }

            int guests = booking.getNumberOfPeople() == null ? 0 : booking.getNumberOfPeople();
            int maxCapacity = restaurant.getCapacity() == null ? 0 : restaurant.getCapacity();

            boolean ok = redisAvailabilityService.reserveRestaurant(restaurant.getId(), reservationTime, guests, maxCapacity);
            if (!ok) {
                log.warn("Warmup Restaurant potential over-capacity: restaurantId={} time={} bookingId={} people={}",
                        restaurant.getId(), reservationTime, booking.getId(), guests);
                // Unlike BNB, we might not want to throw an exception here if restaurant overbooking is handled differently,
                // but for consistency with BnbAvailabilityWarmupRunner, one might consider it.
                // Given the instructions, I'll keep it as a warning to avoid breaking startup if discrepancies exist.
            }
            applied++;
        }

        log.info("Availability warmup (Restaurant) completed: bookingsLoaded={}, bookingsApplied={}",
                bookings.size(), applied);
    }
}
