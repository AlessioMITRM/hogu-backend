package us.hogu.controller.dto.request;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Positive;
import javax.validation.constraints.PositiveOrZero;

import lombok.Data;
import us.hogu.model.enums.PricingType;

@Data
public class EventPricingConfigurationRequestDto {
    
    private Long id;
    
    @NotNull(message = "Il tipo di prezzo è obbligatorio")
    private PricingType pricingType;
    
    @NotBlank(message = "La descrizione della configurazione è obbligatoria")
    private String description;
    
    @NotNull(message = "Il prezzo è obbligatorio")
    @PositiveOrZero(message = "Il prezzo deve essere positivo o zero")
    private Double price;
    
    @Positive(message = "La capacità deve essere positiva")
    private Integer capacity;
    
    private Boolean isActive;
}