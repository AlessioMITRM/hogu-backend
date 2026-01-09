package us.hogu.controller.dto.request;

import java.util.List;

import lombok.Data;

@Data
public class BnbRoomRequestDto {
    private String name;
    private String description;
    private Integer maxGuests;
    private Double basePricePerNight;
    private Boolean available;
    private List<String> images;
}
