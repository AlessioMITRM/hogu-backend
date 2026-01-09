package us.hogu.controller.dto.response;

import java.time.OffsetDateTime;
import java.util.List;

import lombok.Builder;
import lombok.Data;
import us.hogu.controller.dto.response.ServiceLocaleResponseDto;

@Data
@Builder
public class BnbServiceResponseDto {
    private Long id;
    private String name;
    private String description;
    private Double defaultPricePerNight;
    private Integer totalRooms;
    private Integer maxGuestsForRoom;
    private List<String> images;
    private List<ServiceLocaleResponseDto> locales;
    private OffsetDateTime creationDate;
    private Boolean publicationStatus;
    private String providerName;
}

