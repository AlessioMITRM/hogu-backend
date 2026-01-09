package us.hogu.controller.dto.response;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LuggageServiceProviderResponseDto {

    private Long id;

    private String name;

    private String description;

    private String city;

    private String state;

    private String address;

    private Integer capacity;

    private BigDecimal basePrice;

    private Boolean publicationStatus;

    private OffsetDateTime creationDate;

    /** URL completi delle immagini (es. /files/luggage/123/filename.jpg) */
    private List<String> images;

    /** Lista dei prezzi per dimensione */
    private List<LuggageSizePriceResponseDto> sizePrices;

    /** Lista degli orari di apertura (7 giorni) */
    private List<OpeningHourResponseDto> openingHours;
}
