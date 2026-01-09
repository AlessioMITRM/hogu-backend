package us.hogu.controller.dto.common;

import java.time.OffsetDateTime;

import lombok.Data;

@Data
public class ProviderInfoDto {
	private Long id;
	
	private String name;
	
	private String surname;
	
	private String email;
	
	private String status; // PENDING, APPROVED, REJECTED, SUSPENDED
	
	private OffsetDateTime registrationDate;
	
	private Integer servicesCount;
	
	private Double totalEarnings;
	
	private Double pendingPayouts;
	
	private Double totalCommission;
}
