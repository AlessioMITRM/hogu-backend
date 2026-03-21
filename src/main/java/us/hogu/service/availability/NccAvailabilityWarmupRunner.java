package us.hogu.service.availability;

import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.List;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import us.hogu.configuration.properties.AvailabilityProperties;
import us.hogu.model.NccBooking;
import us.hogu.model.NccServiceEntity;
import us.hogu.model.enums.BookingStatus;
import us.hogu.repository.jpa.NccBookingJpa;
import us.hogu.service.redis.RedisAvailabilityService;

@Slf4j
@Component
@RequiredArgsConstructor
public class NccAvailabilityWarmupRunner implements ApplicationRunner {
    private final AvailabilityProperties availabilityProperties;
    private final NccBookingJpa nccBookingJpa;
    private final RedisAvailabilityService redisAvailabilityService;

    @Override
    @Transactional(readOnly = true)
    public void run(ApplicationArguments args) {
        if (!availabilityProperties.getWarmup().isEnabled()) {
            return;
        }

        if (availabilityProperties.getWarmup().getFlushNamespace().isEnabled()) {
            long deletedAvailability = redisAvailabilityService.deleteByPattern("ncc:service:*");
            long deletedWarmupMarks = redisAvailabilityService.deleteByPattern("availability:warmup:applied:*");
            log.info("Availability warmup (NCC): deletedAvailabilityKeys={}, deletedWarmupMarks={}",
                    deletedAvailability, deletedWarmupMarks);
        }

        Collection<BookingStatus> statuses = AvailabilityBookingStatusPolicy.occupyingStatuses();
        List<NccBooking> bookings = nccBookingJpa.findAllByStatusInWithServiceAndVehicles(statuses);

        long applied = 0;
        for (NccBooking booking : bookings) {
            if (booking == null || booking.getId() == null) {
                continue;
            }
            if (!redisAvailabilityService.markWarmupAppliedOnce(booking.getId())) {
                continue;
            }

            NccServiceEntity service = booking.getNccService();
            if (service == null || service.getId() == null) {
                continue;
            }

            OffsetDateTime pickupTime = booking.getPickupTime();
            if (pickupTime == null) {
                continue;
            }

            int maxCapacity = service.getVehiclesAvailable() != null ? service.getVehiclesAvailable().size() : 0;
            if (maxCapacity == 0) {
                continue;
            }

            // NCC reservs 1 vehicle per booking in the current logic
            boolean ok = redisAvailabilityService.reserveNcc(service.getId(), pickupTime, maxCapacity);
            if (!ok) {
                log.warn("Warmup NCC detected over-capacity serviceId={} time={} bookingId={}",
                        service.getId(), pickupTime, booking.getId());
                // We could throw exception like Bnb implementation, but NCC might have different logic.
                // However, following Bnb's lead for consistency:
                throw new IllegalStateException(
                        "Warmup NCC failed: over-capacity serviceId=" + service.getId()
                                + " time=" + pickupTime
                                + " bookingId=" + booking.getId());
            }
            applied++;
        }

        log.info("Availability warmup (NCC) completed: bookingsLoaded={}, bookingsApplied={}",
                bookings.size(), applied);
    }
}
