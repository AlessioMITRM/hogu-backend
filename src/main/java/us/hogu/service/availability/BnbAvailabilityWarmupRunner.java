package us.hogu.service.availability;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import us.hogu.configuration.properties.AvailabilityProperties;
import us.hogu.model.BnbBooking;
import us.hogu.model.BnbRoom;
import us.hogu.model.enums.BookingStatus;
import us.hogu.repository.jpa.BnbBookingJpa;
import us.hogu.service.redis.RedisAvailabilityService;

@Slf4j
@Component
@RequiredArgsConstructor
public class BnbAvailabilityWarmupRunner implements ApplicationRunner {
    private final AvailabilityProperties availabilityProperties;
    private final BnbBookingJpa bnbBookingJpa;
    private final RedisAvailabilityService redisAvailabilityService;

    @Override
    @Transactional(readOnly = true)
    public void run(ApplicationArguments args) {
        if (!availabilityProperties.getWarmup().isEnabled()) {
            return;
        }

        if (availabilityProperties.getWarmup().getFlushNamespace().isEnabled()) {
            long deletedAvailability = redisAvailabilityService.deleteByPattern("bnb:room:*");
            long deletedWarmupMarks = redisAvailabilityService.deleteByPattern("availability:warmup:applied:*");
            log.info("Availability warmup (BNB): deletedAvailabilityKeys={}, deletedWarmupMarks={}",
                    deletedAvailability, deletedWarmupMarks);
        }

        Set<BookingStatus> statuses = AvailabilityBookingStatusPolicy.occupyingStatuses();
        List<BnbBooking> bookings = bnbBookingJpa.findAllByStatusInWithRoom(statuses);

        long applied = 0;
        for (BnbBooking booking : bookings) {
            if (booking == null || booking.getId() == null) {
                continue;
            }
            if (!redisAvailabilityService.markWarmupAppliedOnce(booking.getId())) {
                continue;
            }

            BnbRoom room = booking.getRoom();
            if (room == null || room.getId() == null) {
                continue;
            }

            LocalDate checkIn = booking.getCheckInDate();
            LocalDate checkOut = booking.getCheckOutDate();
            if (checkIn == null || checkOut == null || !checkIn.isBefore(checkOut)) {
                continue;
            }

            int guests = booking.getNumberOfGuests() == null ? 0 : booking.getNumberOfGuests();
            int maxCapacity = room.getMaxGuests() == null ? 0 : room.getMaxGuests();

            LocalDate current = checkIn;
            while (current.isBefore(checkOut)) {
                boolean ok = redisAvailabilityService.reserve(room.getId(), current, guests, maxCapacity);
                if (!ok) {
                    throw new IllegalStateException(
                            "Warmup BNB failed: over-capacity roomId=" + room.getId()
                                    + " date=" + current
                                    + " bookingId=" + booking.getId());
                }
                current = current.plusDays(1);
            }
            applied++;
        }

        log.info("Availability warmup (BNB) completed: bookingsLoaded={}, bookingsApplied={}",
                bookings.size(), applied);
    }
}
