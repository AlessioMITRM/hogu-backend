package us.hogu.controller.dto.request;

import java.util.List;

import javax.validation.constraints.Email;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

import lombok.Data;
import us.hogu.model.enums.ServiceType;

@Data
public class ProviderRegistrationRequestDto {

    @NotBlank
    private String name;

    @Email(message = "Email non valida")
    @NotBlank(message = "L'email è obbligatoria")
    private String email;

    @Size(min = 6, message = "La password deve contenere almeno 6 caratteri")
    private String password;

    @NotNull
    private ServiceType serviceType;

    private List<UserDocumentRequestDto> documents;

    private String description;

    private String language;

    private String state;

    @NotBlank(message = "L'IBAN è obbligatorio")
    private String iban;

    private Long maxCapacity;

    private java.math.BigDecimal basePrice;
}
