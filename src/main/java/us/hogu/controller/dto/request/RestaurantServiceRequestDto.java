package us.hogu.controller.dto.request;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

import lombok.Data;
import us.hogu.model.internal.Menu;

@Data
public class RestaurantServiceRequestDto {
    @NotBlank(message = "Il nome del ristorante è obbligatorio")
    @Size(max = 255, message = "Il nome non può superare i 255 caratteri")
    private String name;

    @NotBlank(message = "La descrizione è obbligatoria")
    @Size(max = 1000, message = "La descrizione non può superare i 1000 caratteri")
    private String description;

    @NotEmpty(message = "È necessario specificare almeno una posizione")
    @Valid
    private List<ServiceLocaleRequestDto> locales;

    private String menu;

    private Integer capacity;

    private BigDecimal basePrice;

    private List<String> images;

    private Boolean publicationStatus;
}
