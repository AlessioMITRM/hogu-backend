package us.hogu.controller.dto.response;

import java.util.Map;

import lombok.Data;

@Data
public class ReviewSummaryResponseDto {
    private Double averageRating;
    
    private Map<Integer, Integer> ratingDistribution; // 5→10, 4→5, ecc.
    
    private Integer totalReviews;
}
