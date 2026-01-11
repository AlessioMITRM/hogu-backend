package us.hogu.controller.dto.request;

import java.math.BigDecimal;
import java.util.List;

import lombok.Data;

@Data
public class BnbServiceRequestDto {
    private String name;
    private String description;
    private BigDecimal defaultPricePerNight;
    private Integer totalRooms;
    private Integer maxGuestsForRoom;
    private List<ServiceLocaleRequestDto> locales;
    private Boolean publcationStatus;
}
