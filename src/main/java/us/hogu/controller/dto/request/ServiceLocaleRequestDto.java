package us.hogu.controller.dto.request;

import io.micrometer.core.lang.NonNull;
import lombok.Data;
import us.hogu.model.enums.ServiceType;

@Data
public class ServiceLocaleRequestDto {

	private Long id;
	
    private ServiceType serviceType;

    private String language;

    private String country;

    private String state;

    private String city;

    private String address;
}
