package us.hogu.service.availability;

import java.util.List;
import java.util.Set;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import us.hogu.configuration.properties.AvailabilityProperties;
import us.hogu.model.ClubBooking;
import us.hogu.model.EventClubServiceEntity;
import us.hogu.model.enums.BookingStatus;
import us.hogu.repository.jpa.ClubBookingJpa;
import us.hogu.service.redis.RedisAvailabilityService;

@Slf4j
@Component
@RequiredArgsConstructor
public class ClubAvailabilityWarmupRunner implements ApplicationRunner {
    private final AvailabilityProperties availabilityProperties;
    private final ClubBookingJpa clubBookingJpa;
    private final RedisAvailabilityService redisAvailabilityService;

    @Override
    @Transactional(readOnly = true)
    public void run(ApplicationArguments args) {
        if (!availabilityProperties.getWarmup().isEnabled()) {
            return;
        }

        if (availabilityProperties.getWarmup().getFlushNamespace().isEnabled()) {
            long deletedAvailability = redisAvailabilityService.deleteByPattern("club:event:*");
            long deletedWarmupMarks = redisAvailabilityService.deleteByPattern("availability:warmup:applied:*");
            log.info("Availability warmup (Club): deletedAvailabilityKeys={}, deletedWarmupMarks={}",
                    deletedAvailability, deletedWarmupMarks);
        }

        Set<BookingStatus> statuses = AvailabilityBookingStatusPolicy.occupyingStatuses();
        List<ClubBooking> bookings = clubBookingJpa.findAllByStatusInWithEvent(statuses);

        long applied = 0;
        for (ClubBooking booking : bookings) {
            if (booking == null || booking.getId() == null) {
                continue;
            }
            if (!redisAvailabilityService.markWarmupAppliedOnce(booking.getId())) {
                continue;
            }

            EventClubServiceEntity event = booking.getEventClubService();
            if (event == null || event.getId() == null) {
                continue;
            }

            int guests = booking.getNumberOfPeople() == null ? 0 : booking.getNumberOfPeople();
            int maxCapacity = event.getMaxCapacity() == null ? 0 : event.getMaxCapacity().intValue();

            // Note: RedisAvailabilityService.reserveEvent uses initialAvailableCapacity as maxCapacity if key doesn't exist.
            // In the context of warmup, we want to decrease the availability by the number of guests.
            // reserveEvent(eventId, people, initialAvailableCapacity)
            boolean ok = redisAvailabilityService.reserveEvent(event.getId(), guests, maxCapacity);
            if (!ok) {
                log.warn("Warmup Club potential over-capacity: eventId={} bookingId={} people={}",
                        event.getId(), booking.getId(), guests);
            }
            applied++;
        }

        log.info("Availability warmup (Club) completed: bookingsLoaded={}, bookingsApplied={}",
                bookings.size(), applied);
    }
}
