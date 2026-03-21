package us.hogu.controller.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;

import lombok.Builder;
import lombok.Data;
import us.hogu.model.enums.BookingStatus;

@Data
@Builder
public class PriceChangeRequestDto {

    private Long bookingId;
    private String bookingCode;

    // Tipo di servizio: "NCC", "BNB", "CLUB", "RESTAURANT", "LUGGAGE"
    private String serviceType;

    private Long serviceId;
    private String serviceName;
    private String serviceImage;
    private String customerName;

    // Nuovo prezzo proposto dal provider (= totalAmount corrente)
    private BigDecimal newPrice;

    // Data rilevante del servizio (per ordinamento) — stringa ISO
    private String serviceDate;

    private BookingStatus status;
    private String statusReason;
    private OffsetDateTime creationDate;

    // --- Campi specifici NCC ---
    private String pickupLocation;
    private String destination;
    private Integer passengers;
    private Double pickupLatitude;
    private Double pickupLongitude;
    private Double destinationLatitude;
    private Double destinationLongitude;
    private OffsetDateTime pickupTime;

    // --- Campi specifici BnB ---
    private String checkInDate;
    private String checkOutDate;
    private Integer numberOfGuests;

    // --- Campi specifici Club / Restaurant ---
    private OffsetDateTime reservationTime;
    private Integer numberOfPeople;

    // --- Campi specifici Luggage ---
    private OffsetDateTime pickUpTime;
    private OffsetDateTime dropOffTime;
    private Integer bagsSmall;
    private Integer bagsMedium;
    private Integer bagsLarge;

    // --- Campo comune ---
    private String specialRequests;
}
