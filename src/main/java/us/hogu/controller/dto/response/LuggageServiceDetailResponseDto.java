package us.hogu.controller.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LuggageServiceDetailResponseDto {

    /** ID del deposito bagagli */
    private Long id;

    /** Nome del punto deposito (es. "Hogu Point - Stazione Termini") */
    private String name;

    /** Descrizione visibile al cliente */
    private String description;

    /** Città */
    private String city;

    /** Regione / Provincia */
    private String state;

    /** Indirizzo completo */
    private String address;

    /** Capienza massima in numero di bagagli */
    private Integer capacity;

    /** Prezzo base (il più basso tra le categorie) */
    private BigDecimal basePrice;

    /** Stato di pubblicazione/visibilità */
    private Boolean publicationStatus;

    /** Data di creazione del deposito */
    private OffsetDateTime creationDate;

    /** URL completi delle immagini del deposito */
    private List<String> images;

    /** Prezzi per dimensione bagaglio */
    private List<LuggageSizePriceResponseDto> sizePrices;

    /** Orari di apertura (uno per ogni giorno della settimana) */
    private List<OpeningHourResponseDto> openingHours;

    /** Informazioni sul provider (opzionale, utile per admin o log) */
    private Long providerId;

    private String providerName; // es. nome attività o nome utente
}