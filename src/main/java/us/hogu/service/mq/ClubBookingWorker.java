package us.hogu.service.mq;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import us.hogu.configuration.RabbitMQConfig;
import us.hogu.controller.dto.request.ClubBookingEvent;
import us.hogu.exception.ResourceNotFoundException;
import us.hogu.model.EventClubServiceEntity;
import us.hogu.repository.jpa.EventClubServiceRepository;

@Slf4j
@Component
@RequiredArgsConstructor
public class ClubBookingWorker {

    private final EventClubServiceRepository eventRepository;

    @RabbitListener(queues = RabbitMQConfig.CLUB_QUEUE_NAME)
    @Transactional
    public void processClubBooking(ClubBookingEvent event) {
        log.info("Processing club booking event: {}", event);

        try {
            // Update occupied capacity atomically
            // Note: Redis has already authorized this, so we just persist the change
            eventRepository.incrementOccupiedCapacity(event.getEventId(), event.getNumberOfPeople());
            
            log.info("Successfully updated availability for club booking user={} event={} added={}", 
                    event.getUserId(), event.getEventId(), event.getNumberOfPeople());

        } catch (Exception e) {
            log.error("Failed to process club booking event: {}", event, e);
            // Retry mechanism handled by RabbitMQ
            throw e;
        }
    }
}
