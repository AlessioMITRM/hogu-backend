package us.hogu.repository.projection;

import java.util.List;
import java.util.Map;

import us.hogu.model.ServiceLocale;

public interface RestaurantSummaryProjection {
    Long getId();
    
    String getName();
    
    String getDescription();
    
    Double getBasePrice();
    
    String getImages();
    
    List<ServiceLocale> getLocales();
}
