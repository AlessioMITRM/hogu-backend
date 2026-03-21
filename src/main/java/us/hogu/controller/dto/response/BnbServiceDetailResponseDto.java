package us.hogu.controller.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BnbServiceDetailResponseDto {

    private Long id;

    private String name;

    private String description;

    private BigDecimal defaultPricePerNight;

    private Integer totalRooms;

    private Integer maxGuestsForRoom;

    private List<ServiceLocaleResponseDto> locales;

    private List<String> images;

    private Boolean publicationStatus;

    private Long providerId;
}
