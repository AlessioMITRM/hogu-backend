package us.hogu.controller.dto.request;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

import io.micrometer.core.lang.NonNull;
import lombok.Data;
import us.hogu.model.enums.ServiceType;

@Data
public class ReviewCreateRequestDto {
	
	@NotNull
	private ServiceType serviceType;
	
	@NonNull
	private Long serviceId;
	
	@NonNull
	private Integer rating;
	
	@NotBlank
	private String review;
}
