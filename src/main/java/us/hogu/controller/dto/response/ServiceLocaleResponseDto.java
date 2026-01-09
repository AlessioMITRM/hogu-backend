package us.hogu.controller.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import us.hogu.model.enums.ServiceType;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ServiceLocaleResponseDto {
	
    private Long serviceId;

    private ServiceType serviceType;

    private String language;  // es. "it", "en", "fr"

    private String country;

    private String state;

    private String city;

    private String address;
}
