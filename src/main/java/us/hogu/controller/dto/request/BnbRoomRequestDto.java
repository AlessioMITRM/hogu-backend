package us.hogu.controller.dto.request;

import java.math.BigDecimal;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonAlias;

import lombok.Data;

@Data
public class BnbRoomRequestDto {
    private String name;
    private String description;
    private Integer maxGuests;
    private BigDecimal priceForNight;

    /** Campo legacy (usato dalla creazione camera) */
    private Boolean available;

    /**
     * Campo inviato dal frontend nel salvataggio aggiornamento camera.
     * Mappa "publicationStatus" dal JSON.
     */
    @JsonAlias("publicationStatus")
    private Boolean publicationStatus;

    private List<String> images;
    private List<BnbRoomPriceRequestDto> priceCalendar;

    /**
     * Restituisce lo stato di pubblicazione effettivo.
     * Usa publicationStatus se presente, altrimenti fallback su available.
     */
    public Boolean getEffectivePublicationStatus() {
        if (publicationStatus != null) return publicationStatus;
        return available;
    }
}
