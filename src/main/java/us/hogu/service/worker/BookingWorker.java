package us.hogu.service.worker;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import us.hogu.configuration.RabbitMQConfig;
import us.hogu.controller.dto.request.BnbBookingEvent;
import us.hogu.model.BnbRoom;
import us.hogu.model.BnbRoomAvailability;
import us.hogu.repository.jpa.BnbRoomAvailabilityJpa;
import us.hogu.repository.jpa.BnbRoomJpa;

import java.time.LocalDate;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class BookingWorker {

    private final BnbRoomAvailabilityJpa availabilityJpa;
    private final BnbRoomJpa roomJpa;

    @RabbitListener(queues = RabbitMQConfig.QUEUE_NAME)
    @Transactional
    public void processBooking(BnbBookingEvent event) {
        log.info("Processing booking event: {}", event);

        // Fetch room to get max capacity if needed
        BnbRoom room = roomJpa.findById(event.getRoomId()).orElse(null);
        if (room == null) {
            log.error("Room {} not found for booking event", event.getRoomId());
            return;
        }

        LocalDate current = event.getCheckIn();
        while (current.isBefore(event.getCheckOut())) {
            updateAvailabilityForDay(room, current, event.getGuests());
            current = current.plusDays(1);
        }
        
        log.info("Booking processed successfully for user {}", event.getUserId());
    }

    private void updateAvailabilityForDay(BnbRoom room, LocalDate date, Integer guests) {
        Optional<BnbRoomAvailability> availabilityOpt = availabilityJpa.findByRoomIdAndDate(room.getId(), date);
        
        BnbRoomAvailability availability;
        if (availabilityOpt.isPresent()) {
            availability = availabilityOpt.get();
            availability.setOccupiedCapacity(availability.getOccupiedCapacity() + guests);
        } else {
            availability = BnbRoomAvailability.builder()
                    .room(room)
                    .date(date)
                    .capacity((long) room.getMaxGuests())
                    .occupiedCapacity((long) guests)
                    .build();
        }
        
        availabilityJpa.save(availability);
    }
}
