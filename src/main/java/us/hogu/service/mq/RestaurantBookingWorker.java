package us.hogu.service.mq;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import us.hogu.configuration.RabbitMQConfig;
import us.hogu.controller.dto.request.RestaurantBookingEvent;
import us.hogu.exception.ResourceNotFoundException;
import us.hogu.model.RestaurantBooking;
import us.hogu.model.enums.BookingStatus;
import us.hogu.repository.jpa.RestaurantBookingJpa;

@Slf4j
@Component
@RequiredArgsConstructor
public class RestaurantBookingWorker {

    private final RestaurantBookingJpa restaurantBookingJpa;

    @RabbitListener(queues = RabbitMQConfig.RESTAURANT_QUEUE_NAME)
    @Transactional
    public void processRestaurantBooking(RestaurantBookingEvent event) {
        log.info("Processing restaurant booking event: {}", event);

        try {
            if (event.getBookingId() != null) {
                RestaurantBooking booking = restaurantBookingJpa.findById(event.getBookingId())
                        .orElseThrow(() -> new ResourceNotFoundException("Booking not found: " + event.getBookingId()));

                if (booking.getStatus() == BookingStatus.PENDING) {
                    booking.setStatus(BookingStatus.WAITING_PROVIDER_CONFIRMATION);
                    restaurantBookingJpa.save(booking);
                    log.info("Updated restaurant booking {} to WAITING_PROVIDER_CONFIRMATION", booking.getId());
                }
            } else {
                log.warn("Received RestaurantBookingEvent without bookingId: {}", event);
            }

        } catch (Exception e) {
            log.error("Failed to process restaurant booking event: {}", event, e);
        }
    }
}
