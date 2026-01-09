package us.hogu.controller.dto.request;

import javax.validation.constraints.NotBlank;

import io.micrometer.core.lang.NonNull;
import lombok.Data;

@Data
public class ReviewUpdateRequestDto {
	@NonNull
	private Long id;
	
	@NonNull
	private Integer rating;
	
	@NotBlank
	private String review;
}
