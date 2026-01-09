package us.hogu.converter;

import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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
            .build();
            
        booking.setServiceType(ServiceType.NCC);
        booking.setStatus(BookingStatus.PENDING);
        booking.setTotalAmount(dto.getTotalAmount());
        booking.setCreationDate(OffsetDateTime.now());
        
        return booking;
    }
    
    // ✅ CLUB BOOKING - Entity from DTO (METODO MANCANTE)
    public ClubBooking toClubEntity(ClubBookingRequestDto dto, User user, ClubServiceEntity clubService) {
        ClubBooking booking = ClubBooking.builder()
            .user(user)
            .clubService(clubService)
            .reservationTime(dto.getReservationTime())
            .numberOfPeople(dto.getNumberOfPeople())
            .specialRequests(dto.getSpecialRequests())
            .build();
            
        booking.setServiceType(ServiceType.CLUB);
        booking.setStatus(BookingStatus.PENDING);
        booking.setTotalAmount(dto.getTotalAmount());
        booking.setCreationDate(OffsetDateTime.now());
        
        return booking;
    }
    
    // ✅ LUGGAGE BOOKING - Entity from DTO (METODO MANCANTE)
    public LuggageBooking toLuggageEntity(LuggageBookingRequestDto dto, User user, LuggageServiceEntity luggageService) {
        LuggageBooking booking = LuggageBooking.builder()
            .user(user)
            .luggageService(luggageService)
            .specialRequests(dto.getSpecialRequests())
            .build();
            
        booking.setServiceType(ServiceType.LUGGAGE);
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
            .status(booking.getStatus())
            .totalAmount(booking.getTotalAmount())
            .creationDate(booking.getCreationDate())
            .reservationTime(booking.getReservationTime())
            .numberOfPeople(booking.getNumberOfPeople())
            .specialRequests(booking.getSpecialRequests())
            .build();
    }
    
    public RestaurantBooking toRestaurantEntity(RestaurantBookingRequestDto dto, User user, RestaurantServiceEntity restaurantServiceEntity) {
        RestaurantBooking booking = RestaurantBooking.builder()
            .user(user)
            .restaurantService(restaurantServiceEntity)
            .reservationTime(dto.getReservationTime())
            .numberOfPeople(dto.getNumberOfPeople())
            .specialRequests(dto.getSpecialRequests())
            .build();
        
        // Imposta campi della classe base Booking
        booking.setServiceType(ServiceType.RESTAURANT);
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
            .build();
    }
    
    public ClubBookingResponseDto toClubResponseDto(ClubBooking booking) {
        return ClubBookingResponseDto.builder()
            .id(booking.getId())
            .status(booking.getStatus())
            .totalAmount(booking.getTotalAmount())
            .creationDate(booking.getCreationDate())
            .reservationTime(booking.getReservationTime())
            .numberOfPeople(booking.getNumberOfPeople())
            .specialRequests(booking.getSpecialRequests())
            .build();
    }
    
    public LuggageBookingResponseDto toLuggageResponseDto(LuggageBooking booking) {
        return LuggageBookingResponseDto.builder()
            .id(booking.getId())
            .serviceType(ServiceType.LUGGAGE)
            .serviceId(booking.getLuggageService().getId())
            .serviceName(booking.getLuggageService().getName())
            .status(booking.getStatus())
            .totalAmount(booking.getTotalAmount())
            .pickUpTime(booking.getPickUpTime())
            .dropOffTime(booking.getDropOffTime())
            .bagsSmall(booking.getBagsSmall())
            .bagsMedium(booking.getBagsMedium())
            .bagsLarge(booking.getBagsLarge())
            .specialRequests(booking.getSpecialRequests())
            .creationDate(booking.getCreationDate())
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