package us.hogu.controller.dto.response;

import java.math.BigDecimal;
import java.util.List;

import us.hogu.controller.dto.response.BnbRoomPriceResponseDto;

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
    private BigDecimal totalPrice;
    private BigDecimal priceForNight;
    private Boolean publicationStatus;
    private List<String> images;
    private List<ServiceLocaleResponseDto> serviceLocale;
    private List<BnbRoomPriceResponseDto> priceCalendar;

    
}

