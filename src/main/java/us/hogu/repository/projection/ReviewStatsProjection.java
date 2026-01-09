package us.hogu.repository.projection;

public interface ReviewStatsProjection {
    Double getAverageRating();
    
    Long getTotalReviews();
    
    Integer getFiveStarCount();
    
    Integer getOneStarCount();
}
