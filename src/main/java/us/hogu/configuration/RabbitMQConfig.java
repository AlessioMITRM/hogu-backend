package us.hogu.configuration;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    public static final String QUEUE_NAME = "booking.queue";
    public static final String EXCHANGE_NAME = "booking.exchange";
    public static final String ROUTING_KEY = "booking.requested";

    public static final String CLUB_QUEUE_NAME = "club.booking.queue";
    public static final String CLUB_EXCHANGE_NAME = "club.booking.exchange";
    public static final String CLUB_ROUTING_KEY = "club.booking.requested";

    public static final String NCC_QUEUE_NAME = "ncc.booking.queue";
    public static final String NCC_EXCHANGE_NAME = "ncc.booking.exchange";
    public static final String NCC_ROUTING_KEY = "ncc.booking.requested";

    public static final String LUGGAGE_QUEUE_NAME = "luggage.booking.queue";
    public static final String LUGGAGE_EXCHANGE_NAME = "luggage.booking.exchange";
    public static final String LUGGAGE_ROUTING_KEY = "luggage.booking.requested";

    public static final String RESTAURANT_QUEUE_NAME = "restaurant.booking.queue";
    public static final String RESTAURANT_EXCHANGE_NAME = "restaurant.booking.exchange";
    public static final String RESTAURANT_ROUTING_KEY = "restaurant.booking.requested";

    @Bean
    public Queue bookingQueue() {
        return new Queue(QUEUE_NAME, true);
    }

    @Bean
    public Queue clubBookingQueue() {
        return new Queue(CLUB_QUEUE_NAME, true);
    }

    @Bean
    public Queue nccBookingQueue() {
        return new Queue(NCC_QUEUE_NAME, true);
    }

    @Bean
    public Queue luggageBookingQueue() {
        return new Queue(LUGGAGE_QUEUE_NAME, true);
    }

    @Bean
    public Queue restaurantBookingQueue() {
        return new Queue(RESTAURANT_QUEUE_NAME, true);
    }

    @Bean
    public TopicExchange bookingExchange() {
        return new TopicExchange(EXCHANGE_NAME);
    }

    @Bean
    public TopicExchange clubBookingExchange() {
        return new TopicExchange(CLUB_EXCHANGE_NAME);
    }

    @Bean
    public TopicExchange nccBookingExchange() {
        return new TopicExchange(NCC_EXCHANGE_NAME);
    }

    @Bean
    public TopicExchange luggageBookingExchange() {
        return new TopicExchange(LUGGAGE_EXCHANGE_NAME);
    }

    @Bean
    public TopicExchange restaurantBookingExchange() {
        return new TopicExchange(RESTAURANT_EXCHANGE_NAME);
    }

    @Bean
    public Binding binding(Queue bookingQueue, TopicExchange bookingExchange) {
        return BindingBuilder.bind(bookingQueue).to(bookingExchange).with(ROUTING_KEY);
    }

    @Bean
    public Binding clubBinding(@Qualifier("clubBookingQueue") Queue queue, @Qualifier("clubBookingExchange") TopicExchange exchange) {
        return BindingBuilder.bind(queue).to(exchange).with(CLUB_ROUTING_KEY);
    }

    @Bean
    public Binding nccBinding(@Qualifier("nccBookingQueue") Queue queue, @Qualifier("nccBookingExchange") TopicExchange exchange) {
        return BindingBuilder.bind(queue).to(exchange).with(NCC_ROUTING_KEY);
    }

    @Bean
    public Binding luggageBinding(@Qualifier("luggageBookingQueue") Queue queue, @Qualifier("luggageBookingExchange") TopicExchange exchange) {
        return BindingBuilder.bind(queue).to(exchange).with(LUGGAGE_ROUTING_KEY);
    }

    @Bean
    public Binding restaurantBinding(@Qualifier("restaurantBookingQueue") Queue queue, @Qualifier("restaurantBookingExchange") TopicExchange exchange) {
        return BindingBuilder.bind(queue).to(exchange).with(RESTAURANT_ROUTING_KEY);
    }
    
    @Bean
    public Jackson2JsonMessageConverter producerJackson2MessageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}
