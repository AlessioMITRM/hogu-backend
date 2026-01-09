package us.hogu.controller.dto.request;

import java.math.BigDecimal;
import java.util.List;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Positive;
import javax.validation.constraints.PositiveOrZero;
import javax.validation.constraints.Size;

import io.micrometer.core.lang.NonNull;
import lombok.Data;

@Data
public class ClubServiceRequestDto {
	
	private Long id;
	
    @NotBlank(message = "Il nome è obbligatorio")
    @Size(min = 2, max = 100, message = "Il nome deve essere tra 2 e 100 caratteri")
    private String name;

    @NotBlank(message = "La descrizione è obbligatoria")
    @Size(min = 10, max = 1000, message = "La descrizione deve essere tra 10 e 1000 caratteri")
    private String description;

    @NotNull
    private List<ServiceLocaleRequestDto> locales;

    @NotNull(message = "La capacità è obbligatoria")
    @Positive(message = "La capacità deve essere un numero positivo")
    private Long maxCapacity;

    @NotNull(message = "Il prezzo base è obbligatorio")
    @PositiveOrZero(message = "Il prezzo base deve essere positivo o zero")
    private BigDecimal basePrice;

    private Boolean publicationStatus;    
}
