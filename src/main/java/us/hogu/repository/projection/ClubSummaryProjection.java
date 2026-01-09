package us.hogu.repository.projection;

import java.util.List;

import us.hogu.model.ServiceLocale;

public interface ClubSummaryProjection {
    Long getId();
    
    String getName();
    
    String getDescription();
    
    Double getBasePrice();
    
    List<ServiceLocale> getLocales();
    
    String getImages();
}
