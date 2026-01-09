package us.hogu.controller.dto.request;

import java.time.OffsetDateTime;
import java.util.List;

import javax.validation.constraints.Future;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Positive;
import javax.validation.constraints.PositiveOrZero;
import javax.validation.constraints.Size;

import lombok.Data;

@Data
public class EventCreateRequestDto {

    private Long eventId;

    @NotBlank(message = "Il nome dell'evento è obbligatorio")
    @Size(min = 2, max = 100, message = "Il nome deve essere tra 2 e 100 caratteri")
    private String name;

    @NotBlank(message = "La descrizione dell'evento è obbligatoria")
    @Size(min = 10, max = 500, message = "La descrizione deve essere tra 10 e 500 caratteri")
    private String description;

    @NotNull(message = "L'orario di inizio è obbligatorio")
    @Future(message = "L'orario di inizio deve essere nel futuro")
    private OffsetDateTime startTime;

    @NotNull(message = "L'orario di fine è obbligatorio")
    @Future(message = "L'orario di fine deve essere nel futuro")
    private OffsetDateTime endTime;

    @NotNull(message = "Il prezzo è obbligatorio")
    @PositiveOrZero(message = "Il prezzo deve essere positivo o zero")
    private Double price;

    @NotNull(message = "Il numero massimo di partecipanti è obbligatorio")
    @Positive(message = "Il numero massimo di partecipanti deve essere positivo")
    private Integer maxParticipants;
    
    @NotNull
    private List<ServiceLocaleRequestDto> locales;
    
    @Size(max = 100, message = "Il nome del DJ non può superare 100 caratteri")
    private String djName;

    @Size(max = 100, message = "Il tema non può superare 100 caratteri")
    private String theme;
    
    private Boolean isActive;

    private List<EventPricingConfigurationRequestDto> pricingConfigurations;
}

