package us.hogu.controller.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RestaurantServiceDetailResponseDto {

    private Long id;

    private String name;

    private String description;

    private List<ServiceLocaleResponseDto> locales;

    private String menu;

    private Integer capacity;

    private BigDecimal basePrice;

    private Boolean publicationStatus;

    private OffsetDateTime creationDate;

    private List<String> images;

    private Long providerId;
}
