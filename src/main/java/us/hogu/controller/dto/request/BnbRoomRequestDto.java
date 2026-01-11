package us.hogu.controller.dto.request;

import java.math.BigDecimal;
import java.util.List;

import lombok.Data;

@Data
public class BnbRoomRequestDto {
    private String name;
    private String description;
    private Integer maxGuests;
    private BigDecimal basePricePerNight;
    private Boolean available;
    private List<String> images;
}
