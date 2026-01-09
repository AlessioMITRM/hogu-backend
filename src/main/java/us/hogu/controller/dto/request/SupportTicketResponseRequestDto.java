package us.hogu.controller.dto.request;

import javax.validation.constraints.NotBlank;

import io.micrometer.core.lang.NonNull;
import lombok.Data;

@Data
public class SupportTicketResponseRequestDto {
	@NonNull
	private Long ticketId;
	
	@NotBlank
	private String response;
	
	@NotBlank
	private String status;
}
