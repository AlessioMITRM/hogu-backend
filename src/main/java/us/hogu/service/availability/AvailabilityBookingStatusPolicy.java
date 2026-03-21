package us.hogu.service.availability;

import java.util.EnumSet;
import java.util.Set;

import us.hogu.model.enums.BookingStatus;

public final class AvailabilityBookingStatusPolicy {
    private AvailabilityBookingStatusPolicy() {
    }

    public static Set<BookingStatus> occupyingStatuses() {
        return EnumSet.of(
                BookingStatus.WAITING_CUSTOMER_PAYMENT,
                BookingStatus.WAITING_PROVIDER_CONFIRMATION,
                BookingStatus.FULL_PAYMENT_COMPLETED,
                BookingStatus.COMPLETED,
                BookingStatus.PAYMENT_AUTHORIZED,
                BookingStatus.PENDING,
                BookingStatus.PROVIDER_LIQUIDATED,
                BookingStatus.COMMISSION_PAID,
                BookingStatus.WAITING_COMPLETION,
                BookingStatus.COMPLETED_BY_ADMIN
        );
    }

    public static Set<BookingStatus> freeingStatuses() {
        return EnumSet.of(
                BookingStatus.CANCELLED_BY_PROVIDER,
                BookingStatus.CANCELLED_BY_ADMIN,
                BookingStatus.REFUNDED_BY_ADMIN
        );
    }
}
