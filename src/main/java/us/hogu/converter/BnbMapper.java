package us.hogu.converter;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import us.hogu.controller.dto.request.BnbRoomPriceRequestDto;
import us.hogu.controller.dto.request.BnbRoomRequestDto;
import us.hogu.controller.dto.request.BnbServiceRequestDto;
import us.hogu.controller.dto.response.BnbBookingResponseDto;
import us.hogu.controller.dto.response.BnbRoomPriceResponseDto;
import us.hogu.controller.dto.response.BnbRoomResponseDto;
import us.hogu.controller.dto.response.BnbServiceResponseDto;
import us.hogu.model.BnbBooking;
import us.hogu.model.BnbRoom;
import us.hogu.model.BnbRoomPriceCalendar;
import us.hogu.model.BnbServiceEntity;

@RequiredArgsConstructor
@Component
public class BnbMapper {

    private final ServiceLocaleMapper serviceLocaleMapper;

    /*
     * ===========================
     * BNB SERVICE
     * ===========================
     */

    public BnbServiceEntity toEntity(BnbServiceRequestDto dto) {
        if (dto == null)
            return null;

        return BnbServiceEntity.builder()
                .name(dto.getName())
                .description(dto.getDescription())
                .defaultPricePerNight(dto.getDefaultPricePerNight())
                .totalRooms(dto.getTotalRooms())
                .maxGuestsForRoom(dto.getMaxGuestsForRoom())
                .build();
    }

    public BnbServiceResponseDto toResponse(BnbServiceEntity entity) {
        if (entity == null)
            return null;

        return BnbServiceResponseDto.builder()
                .id(entity.getId())
                .name(entity.getName())
                .description(entity.getDescription())
                .defaultPricePerNight(entity.getDefaultPricePerNight())
                .totalRooms(entity.getTotalRooms())
                .maxGuestsForRoom(entity.getMaxGuestsForRoom())
                .locales(serviceLocaleMapper.mapEntityToReponse(entity.getLocales()))
                .images(entity.getImages())
                .creationDate(entity.getCreationDate())
                .publicationStatus(entity.getPublicationStatus())
                .providerName(entity.getUser() != null ? entity.getUser().getName() : null)
                .build();
    }

    public List<BnbServiceResponseDto> toResponseList(List<BnbServiceEntity> entities) {
        return entities == null ? List.of() : entities.stream().map(this::toResponse).collect(Collectors.toList());
    }

    /*
     * ===========================
     * BNB ROOM
     * ===========================
     */

    public BnbRoom toRoomEntity(BnbRoomRequestDto dto) {
        if (dto == null)
            return null;

        return BnbRoom.builder()
                .name(dto.getName())
                .description(dto.getDescription())
                .maxGuests(dto.getMaxGuests())
                .basePricePerNight(dto.getPriceForNight())
                .publicationStatus(dto.getAvailable() != null ? dto.getAvailable() : true)
                .images(dto.getImages())
                .build();
    }

    public BnbRoomResponseDto toRoomResponse(BnbRoom entity) {
        if (entity == null)
            return null;

        List<BnbRoomPriceResponseDto> priceCalendarDtos = entity.getPriceCalendar() == null
                ? List.of()
                : entity.getPriceCalendar().stream()
                        .map(this::toRoomPriceResponse)
                        .collect(Collectors.toList());

        return BnbRoomResponseDto.builder()
                .id(entity.getId())
                .images(entity.getImages())
                .name(entity.getName())
                .description(entity.getDescription())
                .maxGuests(entity.getMaxGuests())
                .priceForNight(entity.getBasePricePerNight())
                .publicationStatus(entity.getPublicationStatus())
                .bnbServiceId(entity.getBnbService() != null ? entity.getBnbService().getId() : null)
                .serviceLocale(entity.getBnbService() != null
                        ? serviceLocaleMapper.mapEntityToReponse(entity.getBnbService().getLocales())
                        : List.of())
                .priceCalendar(priceCalendarDtos)
                .build();
    }

    public List<BnbRoomResponseDto> toRoomResponseList(List<BnbRoom> entities) {
        return entities == null ? List.of() : entities.stream().map(this::toRoomResponse).collect(Collectors.toList());
    }

    /*
     * ===========================
     * BNB ROOM PRICE CALENDAR
     * ===========================
     */

    public BnbRoomPriceCalendar toRoomPriceEntity(BnbRoomPriceRequestDto dto) {
        if (dto == null)
            return null;

        return BnbRoomPriceCalendar.builder()
                .startDate(dto.getStartDate())
                .endDate(dto.getEndDate())
                .pricePerNight(dto.getPricePerNight())
                .build();
    }

    public BnbRoomPriceResponseDto toRoomPriceResponse(BnbRoomPriceCalendar entity) {
        if (entity == null)
            return null;

        return BnbRoomPriceResponseDto.builder()
                .id(entity.getId())
                .startDate(entity.getStartDate())
                .endDate(entity.getEndDate())
                .pricePerNight(entity.getPricePerNight())
                .roomId(entity.getRoom() != null ? entity.getRoom().getId() : null)
                .build();
    }

    /*
     * ===========================
     * BOOKING
     * ===========================
     */

    public BnbBookingResponseDto toBookingResponse(BnbBooking booking) {
        if (booking == null)
            return null;

        String firstName = booking.getBillingFirstName() != null && !booking.getBillingFirstName().isBlank()
                ? booking.getBillingFirstName()
                : (booking.getUser() != null ? booking.getUser().getName() : null);
        String lastName = booking.getBillingLastName() != null && !booking.getBillingLastName().isBlank()
                ? booking.getBillingLastName()
                : (booking.getUser() != null ? booking.getUser().getSurname() : null);
        String fullName = null;
        if (firstName != null && !firstName.isBlank() && lastName != null && !lastName.isBlank()) {
            fullName = firstName + " " + lastName;
        } else if (firstName != null && !firstName.isBlank()) {
            fullName = firstName;
        } else if (lastName != null && !lastName.isBlank()) {
            fullName = lastName;
        }

        return BnbBookingResponseDto.builder()
                .id(booking.getId())
                .bookingFullName(fullName)
                .customerFirstName(firstName)
                .customerLastName(lastName)
                .serviceId(booking.getBnbService() != null ? booking.getBnbService().getId() : null)
                .serviceName(booking.getBnbService() != null ? booking.getBnbService().getName() : null)
                .roomId(booking.getRoom() != null ? booking.getRoom().getId() : null)
                .roomName(booking.getRoom() != null ? booking.getRoom().getName() : null)
                .checkInDate(booking.getCheckInDate())
                .checkOutDate(booking.getCheckOutDate())
                .numberOfGuests(booking.getNumberOfGuests())
                .roomImages(booking.getRoom() != null ? booking.getRoom().getImages() : null)
                .status(booking.getStatus())
                .totalAmount(booking.getTotalAmount())
                .creationDate(booking.getCreationDate())
                .bookingCode(booking.getBookingCode())
                .build();
    }

    public List<BnbBookingResponseDto> toBookingResponseList(List<BnbBooking> bookings) {
        return bookings == null ? List.of()
                : bookings.stream().map(this::toBookingResponse).collect(Collectors.toList());
    }
}
