package us.hogu.service.impl;

import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import us.hogu.service.intefaces.CustomerService;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import us.hogu.controller.dto.response.PriceChangeRequestDto;
import us.hogu.model.BnbBooking;
import us.hogu.model.Booking;
import us.hogu.model.ClubBooking;
import us.hogu.model.LuggageBooking;
import us.hogu.model.NccBooking;
import us.hogu.model.RestaurantBooking;
import us.hogu.model.enums.BookingStatus;
import us.hogu.repository.jpa.BookingJpa;

@Service
@RequiredArgsConstructor
@Slf4j
public class CustomerServiceImpl implements CustomerService {

    private final BookingJpa bookingJpa;

    private static final List<BookingStatus> STATUS_ORDER = List.of(
            BookingStatus.FULL_PAYMENT_COMPLETED,
            BookingStatus.WAITING_PROVIDER_CONFIRMATION,
            BookingStatus.PAYMENT_AUTHORIZED,
            BookingStatus.WAITING_COMPLETION,
            BookingStatus.MODIFIED_BY_PROVIDER,
            BookingStatus.CANCELLED_BY_PROVIDER,
            BookingStatus.CANCELLED_BY_ADMIN,
            BookingStatus.REFUNDED_BY_ADMIN,
            BookingStatus.COMPLETED,
            BookingStatus.COMMISSION_PAID,
            BookingStatus.PROVIDER_LIQUIDATED);

    private int getStatusPriority(BookingStatus status) {
        int index = STATUS_ORDER.indexOf(status);
        return index != -1 ? index : Integer.MAX_VALUE;
    }

    private Instant parseDate(String dateStr) {
        if (dateStr == null)
            return null;
        try {
            return Instant.parse(dateStr);
        } catch (Exception e) {
            try {
                return OffsetDateTime.parse(dateStr).toInstant();
            } catch (Exception ex) {
                try {
                    return java.time.LocalDateTime.parse(dateStr).toInstant(java.time.ZoneOffset.UTC);
                } catch (Exception ex2) {
                    return null;
                }
            }
        }
    }

    @Override
    public List<PriceChangeRequestDto> getUpcomingBookings(Long customerId, int page, int size) {
        List<Booking> bookings = bookingJpa.findByUserId(customerId);
        Instant startOfToday = java.time.LocalDate.now().atStartOfDay(java.time.ZoneId.systemDefault()).toInstant();

        List<PriceChangeRequestDto> result = bookings.stream()
                .filter(b -> STATUS_ORDER.contains(b.getStatus()))
                .map(this::mapToDto)
                .filter(dto -> {
                    Instant date = parseDate(dto.getServiceDate());
                    return date != null && !date.isBefore(startOfToday);
                })
                .sorted(Comparator.comparing((PriceChangeRequestDto dto) -> getStatusPriority(dto.getStatus()))
                        .thenComparing(dto -> parseDate(dto.getServiceDate()),
                                Comparator.nullsLast(Comparator.naturalOrder())))
                .collect(Collectors.toList());

        int fromIndex = page * size;
        if (fromIndex >= result.size()) {
            return Collections.emptyList();
        }
        return result.subList(fromIndex, Math.min(fromIndex + size, result.size()));
    }

    @Override
    public List<PriceChangeRequestDto> getPastBookings(Long customerId, int page, int size) {
        List<Booking> bookings = bookingJpa.findByUserId(customerId);
        Instant startOfToday = java.time.LocalDate.now().atStartOfDay(java.time.ZoneId.systemDefault()).toInstant();

        List<PriceChangeRequestDto> result = bookings.stream()
                .filter(b -> STATUS_ORDER.contains(b.getStatus()))
                .map(this::mapToDto)
                .filter(dto -> {
                    Instant date = parseDate(dto.getServiceDate());
                    return date != null && date.isBefore(startOfToday);
                })
                .sorted(Comparator.comparing((PriceChangeRequestDto dto) -> getStatusPriority(dto.getStatus()))
                        .thenComparing(dto -> parseDate(dto.getServiceDate()),
                                Comparator.nullsLast(Comparator.reverseOrder())))
                .collect(Collectors.toList());

        int fromIndex = page * size;
        if (fromIndex >= result.size()) {
            return Collections.emptyList();
        }
        return result.subList(fromIndex, Math.min(fromIndex + size, result.size()));
    }

    private PriceChangeRequestDto mapToDto(Booking b) {
        String customerName = "";
        if (b.getBillingFirstName() != null && b.getBillingLastName() != null) {
            customerName = b.getBillingFirstName() + " " + b.getBillingLastName();
        } else if (b.getUser() != null) {
            customerName = b.getUser().getName() + " " + b.getUser().getSurname();
        }

        PriceChangeRequestDto.PriceChangeRequestDtoBuilder dtoBuilder = PriceChangeRequestDto.builder()
                .bookingId(b.getId())
                .bookingCode(b.getBookingCode())
                .newPrice(b.getTotalAmount())
                .status(b.getStatus())
                .statusReason(b.getStatusReason())
                .creationDate(b.getCreationDate())
                .customerName(customerName)
                .serviceDate(null);

        if (b instanceof NccBooking) {
            NccBooking nb = (NccBooking) b;
            dtoBuilder.serviceType("NCC")
                    .serviceId(nb.getNccService() != null ? nb.getNccService().getId() : null)
                    .serviceName(nb.getNccService() != null ? nb.getNccService().getName() : null)
                    .pickupLocation(nb.getPickupLocation())
                    .destination(nb.getDestination())
                    .passengers(nb.getPassengers())
                    .pickupLatitude(nb.getPickupLatitude())
                    .pickupLongitude(nb.getPickupLongitude())
                    .destinationLatitude(nb.getDestinationLatitude())
                    .destinationLongitude(nb.getDestinationLongitude())
                    .pickupTime(nb.getPickupTime())
                    .serviceDate(nb.getPickupTime() != null ? nb.getPickupTime().toString() : null);

            if (nb.getNccService() != null && nb.getNccService().getImages() != null
                    && !nb.getNccService().getImages().isEmpty()) {
                dtoBuilder.serviceImage(
                        "/files/ncc/" + nb.getNccService().getId() + "/" + nb.getNccService().getImages().get(0));
            }
        } else if (b instanceof BnbBooking) {
            BnbBooking bb = (BnbBooking) b;
            dtoBuilder.serviceType("BNB")
                    .serviceId(bb.getBnbService() != null ? bb.getBnbService().getId() : null)
                    .serviceName(bb.getBnbService() != null ? bb.getBnbService().getName() : null)
                    .checkInDate(bb.getCheckInDate() != null ? bb.getCheckInDate().toString() : null)
                    .checkOutDate(bb.getCheckOutDate() != null ? bb.getCheckOutDate().toString() : null)
                    .numberOfGuests(bb.getNumberOfGuests())
                    .serviceDate(bb.getCheckInDate() != null
                            ? bb.getCheckInDate().atStartOfDay().atOffset(java.time.ZoneOffset.UTC).toString()
                            : null);

            if (bb.getBnbService() != null && bb.getBnbService().getImages() != null
                    && !bb.getBnbService().getImages().isEmpty()) {
                dtoBuilder.serviceImage(
                        "/files/bnb/" + bb.getBnbService().getId() + "/" + bb.getBnbService().getImages().get(0));
            }
        } else if (b instanceof ClubBooking) {
            ClubBooking cb = (ClubBooking) b;
            dtoBuilder.serviceType("CLUB")
                    .serviceId(cb.getClubService() != null ? cb.getClubService().getId() : null)
                    .serviceName(cb.getClubService() != null ? cb.getClubService().getName() : null)
                    .reservationTime(cb.getReservationTime())
                    .numberOfPeople(cb.getNumberOfPeople())
                    .specialRequests(cb.getSpecialRequests())
                    .serviceDate(cb.getReservationTime() != null ? cb.getReservationTime().toString() : null);

            if (cb.getEventClubService() != null && cb.getEventClubService().getImages() != null
                    && !cb.getEventClubService().getImages().isEmpty()) {
                dtoBuilder.serviceImage("/files/club/" + cb.getClubService().getId() + "/event/"
                        + cb.getEventClubService().getId() + "/" + cb.getEventClubService().getImages().get(0));
            }
        } else if (b instanceof RestaurantBooking) {
            RestaurantBooking rb = (RestaurantBooking) b;
            dtoBuilder.serviceType("RESTAURANT")
                    .serviceId(rb.getRestaurantService() != null ? rb.getRestaurantService().getId() : null)
                    .serviceName(rb.getRestaurantService() != null ? rb.getRestaurantService().getName() : null)
                    .reservationTime(rb.getReservationTime())
                    .numberOfPeople(rb.getNumberOfPeople())
                    .specialRequests(rb.getSpecialRequests())
                    .serviceDate(rb.getReservationTime() != null ? rb.getReservationTime().toString() : null);

            if (rb.getRestaurantService() != null && rb.getRestaurantService().getImages() != null
                    && !rb.getRestaurantService().getImages().isEmpty()) {
                dtoBuilder.serviceImage("/files/restaurant/" + rb.getRestaurantService().getId() + "/"
                        + rb.getRestaurantService().getImages().get(0));
            }
        } else if (b instanceof LuggageBooking) {
            LuggageBooking lb = (LuggageBooking) b;
            dtoBuilder.serviceType("LUGGAGE")
                    .serviceId(lb.getLuggageService() != null ? lb.getLuggageService().getId() : null)
                    .serviceName(lb.getLuggageService() != null ? lb.getLuggageService().getName() : null)
                    .pickUpTime(lb.getPickUpTime())
                    .dropOffTime(lb.getDropOffTime())
                    .bagsSmall(lb.getBagsSmall())
                    .bagsMedium(lb.getBagsMedium())
                    .bagsLarge(lb.getBagsLarge())
                    .specialRequests(lb.getSpecialRequests())
                    .serviceDate(lb.getPickUpTime() != null ? lb.getPickUpTime().toString() : null);

            if (lb.getLuggageService() != null && lb.getLuggageService().getImages() != null
                    && !lb.getLuggageService().getImages().isEmpty()) {
                dtoBuilder.serviceImage("/files/luggage/" + lb.getLuggageService().getId() + "/"
                        + lb.getLuggageService().getImages().get(0));
            }
        }

        return dtoBuilder.build();
    }
}
