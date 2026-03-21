package us.hogu.converter;

import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import us.hogu.controller.dto.response.EventPricingConfigurationResponseDto;
import us.hogu.model.enums.PricingType;
import org.springframework.stereotype.Component;

import us.hogu.controller.dto.request.BookingCreateRequestDto;
import us.hogu.controller.dto.request.ClubBookingRequestDto;
import us.hogu.controller.dto.request.LuggageBookingRequestDto;
import us.hogu.controller.dto.request.NccBookingRequestDto;
import us.hogu.controller.dto.request.RestaurantBookingRequestDto;
import us.hogu.controller.dto.response.BookingDetailResponseDto;
import us.hogu.controller.dto.response.ClubBookingResponseDto;
import us.hogu.controller.dto.response.LuggageBookingResponseDto;
import us.hogu.controller.dto.response.NccBookingResponseDto;
import us.hogu.controller.dto.response.RestaurantBookingResponseDto;
import us.hogu.controller.dto.response.UserResponseDto;
import us.hogu.model.Booking;
import us.hogu.model.ClubBooking;
import us.hogu.model.ClubServiceEntity;
import us.hogu.model.LuggageBooking;
import us.hogu.model.LuggageServiceEntity;
import us.hogu.model.NccBooking;
import us.hogu.model.NccServiceEntity;
import us.hogu.model.RestaurantBooking;
import us.hogu.model.RestaurantServiceEntity;
import us.hogu.model.User;
import us.hogu.model.enums.BookingStatus;
import us.hogu.model.enums.ServiceType;

@Component
public class BookingMapper {

    // ✅ NCC BOOKING - Entity from DTO (METODO MANCANTE)
    public NccBooking toNccEntity(NccBookingRequestDto dto, User user, NccServiceEntity nccService) {
        NccBooking booking = NccBooking.builder()
                .user(user)
                .nccService(nccService)
                .pickupTime(dto.getPickupTime())
                .pickupLocation(dto.getPickupLocation())
                .destination(dto.getDestination())
                .passengers(dto.getPassengers())
                .pickupLatitude(dto.getPickupLatitude())
                .pickupLongitude(dto.getPickupLongitude())
                .destinationLatitude(dto.getDestinationLatitude())
                .destinationLongitude(dto.getDestinationLongitude())
                .build();

        booking.setStatus(BookingStatus.PENDING);
        booking.setTotalAmount(dto.getTotalAmount());
        booking.setCreationDate(OffsetDateTime.now());

        return booking;
    }

    // ✅ CLUB BOOKING - Entity from DTO (METODO MANCANTE)
    public ClubBooking toClubEntity(ClubBookingRequestDto dto, User user, ClubServiceEntity clubService,
            java.math.BigDecimal totalAmount) {
        StringBuilder address = new StringBuilder();
        if (dto.getLocale() != null) {
            if (dto.getLocale().getAddress() != null)
                address.append(dto.getLocale().getAddress()).append(" ");
            if (dto.getLocale().getCity() != null)
                address.append(dto.getLocale().getCity()).append(" ");
            if (dto.getLocale().getProvince() != null)
                address.append(dto.getLocale().getProvince()).append(" ");
            if (dto.getLocale().getPostalCode() != null)
                address.append(dto.getLocale().getPostalCode()).append(" ");
            if (dto.getLocale().getCountry() != null)
                address.append(dto.getLocale().getCountry());
        }

        ClubBooking booking = ClubBooking.builder()
                .user(user)
                .clubService(clubService)
                .numberOfPeople(dto.getNumberOfPeople())
                .billingFirstName(dto.getBillingFirstName())
                .billingLastName(dto.getBillingLastName())
                .billingTaxCode(dto.getFiscalCode())
                .billingVatNumber(dto.getTaxId())
                .billingAddress(address.toString().trim())
                .billingEmail(user.getEmail())
                .build();

        booking.setSpecialRequests(dto.getSpecialRequests());
        booking.setStatus(BookingStatus.PENDING);
        booking.setTotalAmount(totalAmount);
        booking.setCreationDate(OffsetDateTime.now());

        return booking;
    }

    // ✅ LUGGAGE BOOKING - Entity from DTO (METODO MANCANTE)
    public LuggageBooking toLuggageEntity(LuggageBookingRequestDto dto, User user,
            LuggageServiceEntity luggageService) {
        LuggageBooking booking = LuggageBooking.builder()
                .user(user)
                .luggageService(luggageService)
                .specialRequests(dto.getSpecialRequests())
                .build();

        booking.setStatus(BookingStatus.PENDING);
        booking.setTotalAmount(dto.getTotalAmount());
        booking.setCreationDate(OffsetDateTime.now());

        return booking;
    }

    public RestaurantBookingResponseDto toRestaurantResponseDto(RestaurantBooking booking) {
        return RestaurantBookingResponseDto.builder()
                .id(booking.getId())
                .serviceType(ServiceType.RESTAURANT)
                .serviceId(booking.getRestaurantService().getId())
                .serviceName(booking.getRestaurantService().getName())
                .bookingFullName(String.format("%s %s",
                        booking.getBillingFirstName() != null ? booking.getBillingFirstName()
                                : (booking.getUser() != null ? booking.getUser().getName() : ""),
                        booking.getBillingLastName() != null ? booking.getBillingLastName()
                                : (booking.getUser() != null ? booking.getUser().getSurname() : ""))
                        .trim())
                .status(booking.getStatus())
                .totalAmount(booking.getTotalAmount())
                .creationDate(booking.getCreationDate())
                .reservationTime(booking.getReservationTime())
                .numberOfPeople(booking.getNumberOfPeople())
                .specialRequests(booking.getSpecialRequests())
                .statusReason(booking.getStatusReason())
                .images(booking.getRestaurantService().getImages())
                .bookingCode(booking.getBookingCode())
                .build();
    }

    public RestaurantBooking toRestaurantEntity(RestaurantBookingRequestDto dto, User user,
            RestaurantServiceEntity restaurantServiceEntity) {
        RestaurantBooking booking = RestaurantBooking.builder()
                .user(user)
                .restaurantService(restaurantServiceEntity)
                .reservationTime(dto.getReservationTime())
                .numberOfPeople(dto.getNumberOfPeople())
                .specialRequests(dto.getSpecialRequests())
                .build();

        // Imposta campi della classe base Booking
        booking.setStatus(BookingStatus.PENDING);
        booking.setTotalAmount(dto.getTotalAmount());
        booking.setCreationDate(OffsetDateTime.now());

        return booking;
    }

    public NccBookingResponseDto toNccResponseDto(NccBooking booking) {
        return NccBookingResponseDto.builder()
                .id(booking.getId())
                .serviceType(ServiceType.NCC)
                .serviceId(booking.getNccService().getId())
                .serviceName(booking.getNccService().getName())
                .status(booking.getStatus())
                .totalAmount(booking.getTotalAmount())
                .creationDate(booking.getCreationDate())
                .pickupTime(booking.getPickupTime())
                .pickupLocation(booking.getPickupLocation())
                .destination(booking.getDestination())
                .passengers(booking.getPassengers())
                .pickupLatitude(booking.getPickupLatitude())
                .pickupLongitude(booking.getPickupLongitude())
                .destinationLatitude(booking.getDestinationLatitude())
                .destinationLongitude(booking.getDestinationLongitude())
                .bookingCode(booking.getBookingCode())
                .build();
    }

    public ClubBookingResponseDto toClubResponseDto(ClubBooking booking) {
        EventPricingConfigurationResponseDto pricingDto = null;
        boolean isTable = false;

        if (booking.getPricingType() != null) {
            try {
                PricingType type = PricingType.valueOf(booking.getPricingType());
                pricingDto = EventPricingConfigurationResponseDto.builder()
                        .pricingType(type)
                        .description(booking.getPricingDescription())
                        .price(booking.getPricingPrice())
                        .build();

                if (type == PricingType.VIP_TABLE || type == PricingType.STANDARD_TABLE) {
                    isTable = true;
                }
            } catch (IllegalArgumentException e) {
                // ignore invalid enum
            }
        }

        String fullName = "";
        if (booking.getBillingFirstName() != null)
            fullName += booking.getBillingFirstName();
        if (booking.getBillingLastName() != null)
            fullName += " " + booking.getBillingLastName();

        return ClubBookingResponseDto.builder()
                .id(booking.getId())
                .eventId(booking.getEventClubService() != null ? booking.getEventClubService().getId() : null)
                .eventName(booking.getEventClubService() != null ? booking.getEventClubService().getName() : null)
                .bookingFullName(fullName.trim())
                .status(booking.getStatus())
                .totalAmount(booking.getTotalAmount())
                .creationDate(booking.getCreationDate())
                .reservationTime(booking.getReservationTime())
                .numberOfPeople(booking.getNumberOfPeople())
                .specialRequests(booking.getSpecialRequests())
                .statusReason(booking.getStatusReason())
                .table(isTable)
                .pricingConfiguration(pricingDto)
                .eventImages(booking.getEventClubService() != null ? booking.getEventClubService().getImages() : null)
                .bookingCode(booking.getBookingCode())
                .build();
    }

    public LuggageBookingResponseDto toLuggageResponseDto(LuggageBooking booking) {
        String firstName = booking.getBillingFirstName();
        String lastName = booking.getBillingLastName();
        if ((firstName == null || firstName.isBlank()) && booking.getUser() != null) {
            firstName = booking.getUser().getName();
        }
        if ((lastName == null || lastName.isBlank()) && booking.getUser() != null) {
            lastName = booking.getUser().getSurname();
        }
        String fullName = null;
        if (firstName != null && !firstName.isBlank() && lastName != null && !lastName.isBlank()) {
            fullName = firstName + " " + lastName;
        } else if (firstName != null && !firstName.isBlank()) {
            fullName = firstName;
        } else if (lastName != null && !lastName.isBlank()) {
            fullName = lastName;
        }
        return LuggageBookingResponseDto.builder()
                .id(booking.getId())
                .serviceType(ServiceType.LUGGAGE)
                .serviceId(booking.getLuggageService().getId())
                .serviceName(booking.getLuggageService().getName())
                .customerFirstName(firstName)
                .customerLastName(lastName)
                .customerName(fullName)
                .status(booking.getStatus())
                .totalAmount(booking.getTotalAmount())
                .pickUpTime(booking.getPickUpTime())
                .dropOffTime(booking.getDropOffTime())
                .bagsSmall(booking.getBagsSmall())
                .bagsMedium(booking.getBagsMedium())
                .bagsLarge(booking.getBagsLarge())
                .specialRequests(booking.getSpecialRequests())
                .creationDate(booking.getCreationDate())
                .bookingCode(booking.getBookingCode())
                .build();
    }

    // ✅ GENERIC METHOD che restituisce il DTO corretto
    public Object toResponseDto(Booking booking) {
        if (booking instanceof RestaurantBooking) {
            return toRestaurantResponseDto((RestaurantBooking) booking);
        } else if (booking instanceof NccBooking) {
            return toNccResponseDto((NccBooking) booking);
        } else if (booking instanceof ClubBooking) {
            return toClubResponseDto((ClubBooking) booking);
        } else if (booking instanceof LuggageBooking) {
            return toLuggageResponseDto((LuggageBooking) booking);
        }
        throw new IllegalArgumentException("Tipo di prenotazione non supportato: " + booking.getClass());
    }

    // ✅ Per liste miste (restituisce List<Object>)
    public List<Object> toResponseDtoList(List<Booking> bookings) {
        return bookings.stream()
                .map(this::toResponseDto)
                .collect(Collectors.toList());
    }
}
