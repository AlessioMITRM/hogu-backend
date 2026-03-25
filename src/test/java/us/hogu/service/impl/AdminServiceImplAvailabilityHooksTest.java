package us.hogu.service.impl;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import us.hogu.model.BnbBooking;
import us.hogu.model.BnbRoom;
import us.hogu.model.BnbServiceEntity;
import us.hogu.model.User;
import us.hogu.model.enums.BookingStatus;
import us.hogu.repository.jpa.BookingJpa;
import us.hogu.repository.jpa.BnbBookingJpa;
import us.hogu.repository.jpa.BnbServiceJpa;
import us.hogu.repository.jpa.ClubServiceJpa;
import us.hogu.repository.jpa.LuggageServiceJpa;
import us.hogu.repository.jpa.NccServiceJpa;
import us.hogu.repository.jpa.RestaurantServiceJpa;
import us.hogu.repository.jpa.UserDocumentJpa;
import us.hogu.repository.jpa.UserJpa;
import us.hogu.repository.jpa.UserOtpJpa;
import us.hogu.repository.jpa.UserServiceVerificationJpa;
import us.hogu.service.intefaces.EmailService;
import us.hogu.service.intefaces.FileService;
import us.hogu.service.redis.RedisAvailabilityService;
import us.hogu.service.redis.UserStatusRedisService;

class AdminServiceImplAvailabilityHooksTest {

    @Test
    void updateBookingStatus_bnbEnteringFreeing_releasesRedisOnce_evenIfCalledTwice() {
        UserJpa userJpa = mock(UserJpa.class);
        UserOtpJpa userOtpJpa = mock(UserOtpJpa.class);
        UserServiceVerificationJpa userServiceVerificationJpa = mock(UserServiceVerificationJpa.class);
        UserDocumentJpa userDocumentJpa = mock(UserDocumentJpa.class);
        NccServiceJpa nccServiceJpa = mock(NccServiceJpa.class);
        RestaurantServiceJpa restaurantServiceJpa = mock(RestaurantServiceJpa.class);
        ClubServiceJpa clubServiceJpa = mock(ClubServiceJpa.class);
        LuggageServiceJpa luggageServiceJpa = mock(LuggageServiceJpa.class);
        BnbServiceJpa bnbServiceJpa = mock(BnbServiceJpa.class);
        BnbBookingJpa bnbBookingJpa = mock(BnbBookingJpa.class);
        BookingJpa bookingJpa = mock(BookingJpa.class);
        EmailService emailService = mock(EmailService.class);
        FileService fileService = mock(FileService.class);
        UserStatusRedisService userStatusRedisService = mock(UserStatusRedisService.class);
        RedisAvailabilityService redisAvailabilityService = mock(RedisAvailabilityService.class);

        AdminServiceImpl adminService = new AdminServiceImpl(
                userJpa,
                userOtpJpa,
                userServiceVerificationJpa,
                userDocumentJpa,
                nccServiceJpa,
                restaurantServiceJpa,
                clubServiceJpa,
                luggageServiceJpa,
                bnbServiceJpa,
                bnbBookingJpa,
                bookingJpa,
                emailService,
                fileService,
                userStatusRedisService,
                redisAvailabilityService);

        User provider = User.builder().id(10L).email("provider@example.com").build();
        User customer = User.builder().id(20L).email("customer@example.com").build();
        BnbServiceEntity bnbService = BnbServiceEntity.builder().user(provider).build();
        BnbRoom room = BnbRoom.builder().id(5L).bnbService(bnbService).build();

        BnbBooking booking = BnbBooking.builder()
                .id(1L)
                .user(customer)
                .status(BookingStatus.PENDING)
                .totalAmount(BigDecimal.TEN)
                .billingAddress("addr")
                .billingEmail("billing@example.com")
                .bnbService(bnbService)
                .room(room)
                .checkInDate(LocalDate.of(2026, 3, 20))
                .checkOutDate(LocalDate.of(2026, 3, 22))
                .numberOfGuests(2)
                .build();

        when(bookingJpa.findById(1L)).thenReturn(Optional.of(booking));
        when(bookingJpa.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(redisAvailabilityService.markBookingReleasedOnce(1L)).thenReturn(true, false);

        adminService.updateBookingStatus(1L, BookingStatus.CANCELLED_BY_ADMIN, "reason");
        adminService.updateBookingStatus(1L, BookingStatus.CANCELLED_BY_ADMIN, "reason");

        verify(redisAvailabilityService, times(1)).rollbackBnb(
                eq(5L),
                eq(LocalDate.of(2026, 3, 20)),
                eq(LocalDate.of(2026, 3, 22)),
                eq(2));
    }
}

