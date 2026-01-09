package us.hogu.controller.dto.response;

import java.time.OffsetDateTime;

import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
public class ProviderSummaryResponseDto {
	private Long id;
	
	private String name;
	
	private String email;
	
	private Double averageRating;
	
	private Integer totalReviews;
	
	private Integer totalServices;
	
	private OffsetDateTime memberSince;
}
