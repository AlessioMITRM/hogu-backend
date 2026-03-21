package us.hogu.service.impl;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import us.hogu.client.feign.dto.response.PaymentResponseDto;
import us.hogu.event.PaymentStatusChangedEvent;
import us.hogu.model.BnbBooking;
import us.hogu.model.BnbRoom;
import us.hogu.model.BnbServiceEntity;
import us.hogu.model.Payment;
import us.hogu.model.User;
import us.hogu.model.enums.BookingStatus;
import us.hogu.model.enums.PaymentMethod;
import us.hogu.model.enums.PaymentStatus;
import us.hogu.model.enums.ServiceType;
import us.hogu.repository.jpa.BnbBookingJpa;
import us.hogu.repository.jpa.BookingJpa;
import us.hogu.repository.jpa.ClubBookingJpa;
import us.hogu.repository.jpa.LuggageBookingJpa;
import us.hogu.repository.jpa.NccBookingJpa;
import us.hogu.repository.jpa.PaymentJpa;
import us.hogu.repository.jpa.RestaurantBookingJpa;
import us.hogu.repository.jpa.UserJpa;
import us.hogu.service.intefaces.CommissionService;
import us.hogu.service.intefaces.PayPalService;
import us.hogu.service.intefaces.StripeService;
import us.hogu.service.redis.RedisAvailabilityService;

class PaymentServiceImplAvailabilityHooksTest {

    @Test
    void cancelBookingByProvider_releasesRedisOnlyOnce_evenIfCalledTwice() {
        PaymentJpa paymentJpa = mock(PaymentJpa.class);
        BookingJpa bookingJpa = mock(BookingJpa.class);
        RestaurantBookingJpa restaurantBookingJpa = mock(RestaurantBookingJpa.class);
        NccBookingJpa nccBookingJpa = mock(NccBookingJpa.class);
        ClubBookingJpa clubBookingJpa = mock(ClubBookingJpa.class);
        LuggageBookingJpa luggageBookingJpa = mock(LuggageBookingJpa.class);
        BnbBookingJpa bnbBookingJpa = mock(BnbBookingJpa.class);
        UserJpa userJpa = mock(UserJpa.class);
        StripeService stripeService = mock(StripeService.class);
        PayPalService payPalService = mock(PayPalService.class);
        CommissionService commissionService = mock(CommissionService.class);
        RedisAvailabilityService redisAvailabilityService = mock(RedisAvailabilityService.class);

        PaymentServiceImpl service = new PaymentServiceImpl(
                paymentJpa,
                bookingJpa,
                restaurantBookingJpa,
                nccBookingJpa,
                clubBookingJpa,
                luggageBookingJpa,
                bnbBookingJpa,
                userJpa,
                stripeService,
                payPalService,
                commissionService,
                redisAvailabilityService);

        User provider = User.builder().id(10L).email("provider@example.com").build();
        User customer = User.builder().id(20L).email("customer@example.com").build();
        BnbServiceEntity bnbService = BnbServiceEntity.builder().user(provider).build();
        BnbRoom room = BnbRoom.builder().id(5L).bnbService(bnbService).build();

        BnbBooking booking = BnbBooking.builder()
                .id(1L)
                .user(customer)
                .status(BookingStatus.WAITING_CUSTOMER_PAYMENT)
                .totalAmount(BigDecimal.TEN)
                .billingAddress("addr")
                .billingEmail("billing@example.com")
                .bnbService(bnbService)
                .room(room)
                .checkInDate(LocalDate.of(2026, 3, 20))
                .checkOutDate(LocalDate.of(2026, 3, 22))
                .numberOfGuests(2)
                .build();

        Payment payment = Payment.builder()
                .id(99L)
                .booking(booking)
                .user(customer)
                .paymentMethod(PaymentMethod.STRIPE)
                .paymentIdIntent("pi_test")
                .status(PaymentStatus.AUTHORIZED)
                .build();

        when(bnbBookingJpa.findById(1L)).thenReturn(Optional.of(booking));
        when(bnbBookingJpa.save(any(BnbBooking.class))).thenAnswer(inv -> inv.getArgument(0));
        when(paymentJpa.findByBooking_Id(1L)).thenReturn(List.of(payment));
        when(paymentJpa.save(any(Payment.class))).thenAnswer(inv -> inv.getArgument(0));
        when(stripeService.voidPayment(anyString())).thenReturn(PaymentResponseDto.builder().build());
        when(redisAvailabilityService.markBookingReleasedOnce(1L)).thenReturn(true, false);

        service.cancelBookingByProvider(1L, ServiceType.BNB, 10L, "reason");
        service.cancelBookingByProvider(1L, ServiceType.BNB, 10L, "reason");

        verify(redisAvailabilityService, times(1)).rollbackBnb(
                eq(5L),
                eq(LocalDate.of(2026, 3, 20)),
                eq(LocalDate.of(2026, 3, 22)),
                eq(2));
    }

    @Test
    void paymentFailedEvent_setsBookingCancelledAndReleasesRedis_idempotent() {
        PaymentJpa paymentJpa = mock(PaymentJpa.class);
        BookingJpa bookingJpa = mock(BookingJpa.class);
        RestaurantBookingJpa restaurantBookingJpa = mock(RestaurantBookingJpa.class);
        NccBookingJpa nccBookingJpa = mock(NccBookingJpa.class);
        ClubBookingJpa clubBookingJpa = mock(ClubBookingJpa.class);
        LuggageBookingJpa luggageBookingJpa = mock(LuggageBookingJpa.class);
        BnbBookingJpa bnbBookingJpa = mock(BnbBookingJpa.class);
        UserJpa userJpa = mock(UserJpa.class);
        StripeService stripeService = mock(StripeService.class);
        PayPalService payPalService = mock(PayPalService.class);
        CommissionService commissionService = mock(CommissionService.class);
        RedisAvailabilityService redisAvailabilityService = mock(RedisAvailabilityService.class);

        PaymentServiceImpl service = new PaymentServiceImpl(
                paymentJpa,
                bookingJpa,
                restaurantBookingJpa,
                nccBookingJpa,
                clubBookingJpa,
                luggageBookingJpa,
                bnbBookingJpa,
                userJpa,
                stripeService,
                payPalService,
                commissionService,
                redisAvailabilityService);

        User provider = User.builder().id(10L).email("provider@example.com").build();
        User customer = User.builder().id(20L).email("customer@example.com").build();
        BnbServiceEntity bnbService = BnbServiceEntity.builder().user(provider).build();
        BnbRoom room = BnbRoom.builder().id(5L).bnbService(bnbService).build();

        BnbBooking booking = BnbBooking.builder()
                .id(1L)
                .user(customer)
                .status(BookingStatus.WAITING_CUSTOMER_PAYMENT)
                .totalAmount(BigDecimal.TEN)
                .billingAddress("addr")
                .billingEmail("billing@example.com")
                .bnbService(bnbService)
                .room(room)
                .checkInDate(LocalDate.of(2026, 3, 20))
                .checkOutDate(LocalDate.of(2026, 3, 22))
                .numberOfGuests(2)
                .build();

        Payment payment = Payment.builder()
                .id(99L)
                .booking(booking)
                .user(customer)
                .paymentMethod(PaymentMethod.STRIPE)
                .paymentIdIntent("pi_test")
                .status(PaymentStatus.FAILED)
                .build();

        when(bnbBookingJpa.findById(1L)).thenReturn(Optional.of(booking));
        when(bnbBookingJpa.save(any(BnbBooking.class))).thenAnswer(inv -> inv.getArgument(0));
        when(redisAvailabilityService.markBookingReleasedOnce(1L)).thenReturn(true, false);

        PaymentStatusChangedEvent event = new PaymentStatusChangedEvent(this, payment);

        service.handlePaymentStatusChange(event);
        service.handlePaymentStatusChange(event);

        verify(bnbBookingJpa, times(1)).save(any(BnbBooking.class));
        verify(redisAvailabilityService, times(1)).rollbackBnb(
                eq(5L),
                eq(LocalDate.of(2026, 3, 20)),
                eq(LocalDate.of(2026, 3, 22)),
                eq(2));
    }
}

