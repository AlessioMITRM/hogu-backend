package us.hogu.controller.dto.request;

import java.math.BigDecimal;
import java.util.List;

import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LuggageServiceRequestDto {

    @NotBlank(message = "Il nome del deposito è obbligatorio")
    private String name;

    @NotBlank(message = "La descrizione è obbligatoria")
    private String description;

    @NotNull(message = "Almeno una localizzazione è obbligatoria")
    @Size(min = 1, message = "Deve essere fornita almeno una localizzazione")
    private List<ServiceLocaleRequestDto> locales;

    @NotNull(message = "La capienza massima è obbligatoria")
    @Min(value = 1, message = "La capienza deve essere almeno 1")
    private Integer capacity;

    @NotNull(message = "I prezzi per dimensione sono obbligatori")
    @Size(min = 3, max = 3, message = "Devono essere forniti esattamente 3 prezzi (SMALL, MEDIUM, LARGE)")
    private List<LuggageSizePriceRequestDto> sizePrices;

    @NotNull(message = "Gli orari di apertura sono obbligatori")
    @Size(min = 7, max = 7, message = "Devono essere forniti gli orari per tutti i 7 giorni della settimana")
    private List<OpeningHourRequestDto> openingHours;

    @NotNull(message = "Il basePrice è obbligatorio")
    @DecimalMin(value = "0.0", inclusive = false, message = "Il prezzo base deve essere maggiore di 0")
    private BigDecimal basePrice;

    @NotNull(message = "Lo stato di pubblicazione è obbligatorio")
    private Boolean publicationStatus;
}