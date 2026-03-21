package us.hogu.service.mq;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import us.hogu.configuration.RabbitMQConfig;
import us.hogu.controller.dto.request.LuggageBookingEvent;
import us.hogu.exception.ResourceNotFoundException;
import us.hogu.model.LuggageBooking;
import us.hogu.model.enums.BookingStatus;
import us.hogu.repository.jpa.LuggageBookingJpa;

@Slf4j
@Component
@RequiredArgsConstructor
public class LuggageBookingWorker {

    private final LuggageBookingJpa luggageBookingJpa;

    @RabbitListener(queues = RabbitMQConfig.LUGGAGE_QUEUE_NAME)
    @Transactional
    public void processLuggageBooking(LuggageBookingEvent event) {
        log.info("Processing luggage booking event: {}", event);

        try {
            if (event.getBookingId() != null) {
                LuggageBooking booking = luggageBookingJpa.findById(event.getBookingId())
                        .orElseThrow(() -> new ResourceNotFoundException("Booking not found: " + event.getBookingId()));

                if (booking.getStatus() == BookingStatus.PENDING) {
                    booking.setStatus(BookingStatus.WAITING_PROVIDER_CONFIRMATION);
                    luggageBookingJpa.save(booking);
                    log.info("Updated luggage booking {} to WAITING_PROVIDER_CONFIRMATION", booking.getId());
                }
            } else {
                log.warn("Received LuggageBookingEvent without bookingId: {}", event);
            }

        } catch (Exception e) {
            log.error("Failed to process luggage booking event: {}", event, e);
        }
    }
}
