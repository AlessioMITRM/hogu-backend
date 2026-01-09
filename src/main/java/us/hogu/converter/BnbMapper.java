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

    /* ===========================
     *        BNB SERVICE
     * =========================== */

    public BnbServiceEntity toEntity(BnbServiceRequestDto dto) {
        if (dto == null) return null;

        return BnbServiceEntity.builder()
                .name(dto.getName())
                .description(dto.getDescription())
                .defaultPricePerNight(dto.getDefaultPricePerNight())
                .totalRooms(dto.getTotalRooms())
                .maxGuestsForRoom(dto.getMaxGuestsForRoom())
                .build();
    }

    public BnbServiceResponseDto toResponse(BnbServiceEntity entity) {
        if (entity == null) return null;

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

    /* ===========================
     *        BNB ROOM
     * =========================== */

    public BnbRoom toRoomEntity(BnbRoomRequestDto dto) {
        if (dto == null) return null;

        return BnbRoom.builder()
                .name(dto.getName())
                .description(dto.getDescription())
                .maxGuests(dto.getMaxGuests())
                .basePricePerNight(dto.getBasePricePerNight())
                .available(dto.getAvailable() != null ? dto.getAvailable() : true)
                .images(dto.getImages())
                .build();
    }

    public BnbRoomResponseDto toRoomResponse(BnbRoom entity) {
        if (entity == null) return null;

        return BnbRoomResponseDto.builder()
                .id(entity.getId())
                .name(entity.getName())
                .description(entity.getDescription())
                .maxGuests(entity.getMaxGuests())
                .priceForNight(entity.getBasePricePerNight())
                .available(entity.getAvailable())
                .bnbServiceId(entity.getBnbService() != null ? entity.getBnbService().getId() : null)
                .build();
    }

    public List<BnbRoomResponseDto> toRoomResponseList(List<BnbRoom> entities) {
        return entities == null ? List.of() : entities.stream().map(this::toRoomResponse).collect(Collectors.toList());
    }

    /* ===========================
     *  BNB ROOM PRICE CALENDAR
     * =========================== */

    public BnbRoomPriceCalendar toRoomPriceEntity(BnbRoomPriceRequestDto dto) {
        if (dto == null) return null;

        return BnbRoomPriceCalendar.builder()
                .startDate(dto.getStartDate())
                .endDate(dto.getEndDate())
                .pricePerNight(dto.getPricePerNight())
                .build();
    }

    public BnbRoomPriceResponseDto toRoomPriceResponse(BnbRoomPriceCalendar entity) {
        if (entity == null) return null;

        return BnbRoomPriceResponseDto.builder()
                .id(entity.getId())
                .startDate(entity.getStartDate())
                .endDate(entity.getEndDate())
                .pricePerNight(entity.getPricePerNight())
                .roomId(entity.getRoom() != null ? entity.getRoom().getId() : null)
                .build();
    }

    /* ===========================
     *        BOOKING
     * =========================== */

    public BnbBookingResponseDto toBookingResponse(BnbBooking booking) {
        if (booking == null) return null;

        return BnbBookingResponseDto.builder()
                .id(booking.getId())
                .serviceId(booking.getBnbService() != null ? booking.getBnbService().getId() : null)
                .serviceName(booking.getBnbService() != null ? booking.getBnbService().getName() : null)
                .roomId(booking.getRoom() != null ? booking.getRoom().getId() : null)
                .roomName(booking.getRoom() != null ? booking.getRoom().getName() : null)
                .checkInDate(booking.getCheckInDate())
                .checkOutDate(booking.getCheckOutDate())
                .numberOfGuests(booking.getNumberOfGuests())
                .status(booking.getStatus())
                .totalAmount(booking.getTotalAmount())
                .creationDate(booking.getCreationDate())
                .build();
    }

    public List<BnbBookingResponseDto> toBookingResponseList(List<BnbBooking> bookings) {
        return bookings == null ? List.of() : bookings.stream().map(this::toBookingResponse).collect(Collectors.toList());
    }
}
