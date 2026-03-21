package us.hogu.service.mq;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import us.hogu.configuration.RabbitMQConfig;
import us.hogu.controller.dto.request.NccBookingEvent;
import us.hogu.exception.ResourceNotFoundException;
import us.hogu.model.NccBooking;
import us.hogu.model.enums.BookingStatus;
import us.hogu.repository.jpa.NccBookingJpa;

@Slf4j
@Component
@RequiredArgsConstructor
public class NccBookingWorker {

    private final NccBookingJpa nccBookingJpa;

    @RabbitListener(queues = RabbitMQConfig.NCC_QUEUE_NAME)
    @Transactional
    public void processNccBooking(NccBookingEvent event) {
        log.info("Processing ncc booking event: {}", event);

        try {
            // We fetch the booking created in the service (PENDING)
            // and advance its status or perform post-processing.
            if (event.getBookingId() != null) {
                NccBooking booking = nccBookingJpa.findById(event.getBookingId())
                        .orElseThrow(() -> new ResourceNotFoundException("Booking not found: " + event.getBookingId()));

                // Example: Automatically confirm or move to WAITING_PROVIDER_CONFIRMATION
                // This mimics the "Async Processing" requested.
                // If the previous state was PENDING, we verify and advance.
                if (booking.getStatus() == BookingStatus.PENDING) {
                    booking.setStatus(BookingStatus.WAITING_PROVIDER_CONFIRMATION);
                    nccBookingJpa.save(booking);
                    log.info("Updated ncc booking {} to WAITING_PROVIDER_CONFIRMATION", booking.getId());
                }
            } else {
                log.warn("Received NccBookingEvent without bookingId: {}", event);
            }

        } catch (Exception e) {
            log.error("Failed to process ncc booking event: {}", event, e);
            // RabbitMQ will retry if we throw exception, or we can catch and log.
        }
    }
}
