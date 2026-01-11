package us.hogu.controller.dto.request;

import java.math.BigDecimal;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

import lombok.Data;

@Data
public class RefundRequestDto {
	@NotNull
	private Long paymentId;
	
	@NotNull
	private BigDecimal amount;
	
	@NotBlank
	private String reason;
}
