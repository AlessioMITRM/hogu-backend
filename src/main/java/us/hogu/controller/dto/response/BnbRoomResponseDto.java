package us.hogu.controller.dto.response;

import java.util.List;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class BnbRoomResponseDto {
    private Long id;
    private Long bnbServiceId;
    private String name;
    private String providerName;
    private String description;
    private Integer maxGuests;
    private Double totalPrice;
    private Double priceForNight;
    private Boolean available;
    private List<String> images;
    private List<ServiceLocaleResponseDto> serviceLocale;

    
}

