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
import us.hogu.model.LuggageBooking;
import us.hogu.model.LuggageServiceEntity;
import us.hogu.model.enums.BookingStatus;
import us.hogu.repository.jpa.LuggageBookingJpa;
import us.hogu.service.redis.RedisAvailabilityService;

@Slf4j
@Component
@RequiredArgsConstructor
public class LuggageAvailabilityWarmupRunner implements ApplicationRunner {
    private final AvailabilityProperties availabilityProperties;
    private final LuggageBookingJpa luggageBookingJpa;
    private final RedisAvailabilityService redisAvailabilityService;

    @Override
    @Transactional(readOnly = true)
    public void run(ApplicationArguments args) {
        if (!availabilityProperties.getWarmup().isEnabled()) {
            return;
        }

        if (availabilityProperties.getWarmup().getFlushNamespace().isEnabled()) {
            long deletedAvailability = redisAvailabilityService.deleteByPattern("luggage:service:*");
            long deletedWarmupMarks = redisAvailabilityService.deleteByPattern("availability:warmup:applied:*");
            log.info("Availability warmup (Luggage): deletedAvailabilityKeys={}, deletedWarmupMarks={}",
                    deletedAvailability, deletedWarmupMarks);
        }

        Set<BookingStatus> statuses = AvailabilityBookingStatusPolicy.occupyingStatuses();
        List<LuggageBooking> bookings = luggageBookingJpa.findAllByStatusInWithService(statuses);

        long applied = 0;
        for (LuggageBooking booking : bookings) {
            if (booking == null || booking.getId() == null) {
                continue;
            }
            if (!redisAvailabilityService.markWarmupAppliedOnce(booking.getId())) {
                continue;
            }

            LuggageServiceEntity service = booking.getLuggageService();
            if (service == null || service.getId() == null) {
                continue;
            }

            int bagsSmall = booking.getBagsSmall() != null ? booking.getBagsSmall() : 0;
            int bagsMedium = booking.getBagsMedium() != null ? booking.getBagsMedium() : 0;
            int bagsLarge = booking.getBagsLarge() != null ? booking.getBagsLarge() : 0;
            int totalBags = bagsSmall + bagsMedium + bagsLarge;

            if (totalBags <= 0) {
                continue;
            }

            int maxCapacity = service.getCapacity() != null ? service.getCapacity() : 0;

            boolean ok = redisAvailabilityService.reserveLuggage(
                    service.getId(),
                    booking.getDropOffTime(),
                    booking.getPickUpTime(),
                    totalBags,
                    maxCapacity);

            if (!ok) {
                log.warn("Warmup Luggage failed: over-capacity serviceId={} bookingId={}", 
                        service.getId(), booking.getId());
                // We don't throw exception here to avoid stopping the whole warmup, 
                // but we log it as a warning since overbooking shouldn't happen during warmup unless data is inconsistent.
            } else {
                applied++;
            }
        }

        log.info("Availability warmup (Luggage) completed: bookingsLoaded={}, bookingsApplied={}",
                bookings.size(), applied);
    }
}
