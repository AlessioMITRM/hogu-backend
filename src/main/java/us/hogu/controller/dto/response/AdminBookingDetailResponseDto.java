package us.hogu.controller.dto.response;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import us.hogu.model.enums.BookingStatus;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AdminBookingDetailResponseDto {
    // Dati generici
    private Long id;
    private String bookingCode;
    private BookingStatus status;
    private String statusReason;
    private BigDecimal totalAmount;
    private OffsetDateTime creationDate;

    // Dati Customer / Utente
    private String customerName;
    private String customerEmail;
    private String customerPhone;

    // Dati Provider / Billing
    private String providerName;

    // Dati del servizio
    private String serviceType;
    private String serviceName;
    private String serviceImage;

    // Dati variabili (in base al tipo di servizio)
    private String serviceDate;
    private String address;
    private Integer guests;
    private String providerNote;
    private String specialRequests;

    // Campi specifici opzionali
    private String pickupTime;
    private String pickupLocation;
    private String destination;
    private String dropOffTime;

    private List<String> additionalImages;
}
