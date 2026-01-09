package us.hogu.controller.dto.request;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

import lombok.Data;

@Data
public class RefundRequestDto {
	@NotNull
	private Long paymentId;
	
	@NotNull
	private Double amount;
	
	@NotBlank
	private String reason;
}
