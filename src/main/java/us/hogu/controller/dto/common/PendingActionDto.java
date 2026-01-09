package us.hogu.controller.dto.common;

import java.time.OffsetDateTime;

import lombok.Data;

@Data
public class PendingActionDto {
	private String type; // PROVIDER_APPROVAL, SERVICE_REVIEW, SUPPORT_TICKET, REFUND_REQUEST
	
	private Long idEntity;
	
	private String entityName;
	
	private String description;
	
	private OffsetDateTime createdDate;
	
	private String priority; // LOW, MEDIUM, HIGH
	
	private Integer daysPending;
}
