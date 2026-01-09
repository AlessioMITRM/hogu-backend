package us.hogu.controller.dto.response;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class EventPublicResponseDto {
    private Long id;
    private String name;
    private String location;
    private String description;
    private String djName;
    private String theme;
    private String clubName;
    private String city;
    private String address;
    List<String> images;
    private BigDecimal price;       // Prezzo generico (display)
    private BigDecimal priceMan;    // Prezzo specifico Uomo
    private BigDecimal priceWoman;  // Prezzo specifico Donna
    private BigDecimal tableMinPrice; // Prezzo minimo Tavolo
    private OffsetDateTime startTime;
    private OffsetDateTime endTime;
    private String eventType;
}
