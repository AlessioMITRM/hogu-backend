package us.hogu.controller.dto.response;

import java.math.BigDecimal;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LuggageSearchResultResponseDto {
    private Long id;
    private String name;
    private String description;
    private String address;
    private String location; // Città
    private BigDecimal price;    // Prezzo base o calcolato
    private String imageUrl;
}
