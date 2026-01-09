package us.hogu.controller.dto.response;

import java.util.List;

import lombok.Data;

@Data
public class ReviewListResponseDto {
	private List<ReviewResponseDto> reviews;
    
	private Double averageRating;
    
	private Integer totalReviews;
}
