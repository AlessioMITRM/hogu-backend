package us.hogu.service.mq;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;
import us.hogu.configuration.RabbitMQConfig;
import us.hogu.controller.dto.request.BnbBookingEvent;
import us.hogu.controller.dto.request.ClubBookingEvent;
import us.hogu.controller.dto.request.LuggageBookingEvent;
import us.hogu.controller.dto.request.NccBookingEvent;
import us.hogu.controller.dto.request.RestaurantBookingEvent;

@Slf4j
@Service
@RequiredArgsConstructor
public class BookingProducer {

    private final RabbitTemplate rabbitTemplate;

    public void sendBookingRequest(BnbBookingEvent event) {
        log.info("Sending booking event for user {} room {}", event.getUserId(), event.getRoomId());
        rabbitTemplate.convertAndSend(RabbitMQConfig.EXCHANGE_NAME, RabbitMQConfig.ROUTING_KEY, event);
    }

    public void sendClubBookingRequest(ClubBookingEvent event) {
        log.info("Sending club booking event for user {} event {}", event.getUserId(), event.getEventId());
        rabbitTemplate.convertAndSend(RabbitMQConfig.CLUB_EXCHANGE_NAME, RabbitMQConfig.CLUB_ROUTING_KEY, event);
    }

    public void sendNccBookingRequest(NccBookingEvent event) {
        log.info("Sending ncc booking event for user {} service {}", event.getUserId(), event.getNccServiceId());
        rabbitTemplate.convertAndSend(RabbitMQConfig.NCC_EXCHANGE_NAME, RabbitMQConfig.NCC_ROUTING_KEY, event);
    }

    public void sendLuggageBookingRequest(LuggageBookingEvent event) {
        log.info("Sending luggage booking event for user {} service {}", event.getUserId(), event.getLuggageServiceId());
        rabbitTemplate.convertAndSend(RabbitMQConfig.LUGGAGE_EXCHANGE_NAME, RabbitMQConfig.LUGGAGE_ROUTING_KEY, event);
    }

    public void sendRestaurantBookingRequest(RestaurantBookingEvent event) {
        log.info("Sending restaurant booking event for user {} service {}", event.getUserId(), event.getRestaurantServiceId());
        rabbitTemplate.convertAndSend(RabbitMQConfig.RESTAURANT_EXCHANGE_NAME, RabbitMQConfig.RESTAURANT_ROUTING_KEY, event);
    }
}
