package us.hogu.controller.dto.request;

import javax.validation.constraints.NotBlank;

import lombok.Data;

@Data
public class StaticContentUpdateRequestDto {
	@NotBlank
	private String pageName;
	
	@NotBlank
	private String content;
	
	@NotBlank
	private String title;
}
