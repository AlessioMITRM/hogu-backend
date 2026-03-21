package us.hogu.controller.dto.request;

import io.micrometer.core.lang.NonNull;
import javax.validation.constraints.NotBlank;
import lombok.Data;
import us.hogu.model.enums.ServiceType;

@Data
public class ServiceLocaleRequestDto {

    private Long id;

    private ServiceType serviceType;

    private String language;

    private String country;

    private String state;

    private String province;

    private String postalCode;

    @NotBlank(message = "La città è obbligatoria")
    private String city;

    @NotBlank(message = "L'indirizzo è obbligatorio")
    private String address;
}
