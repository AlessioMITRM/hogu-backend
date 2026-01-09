package us.hogu.controller.dto.request;

import lombok.Data;

import java.time.OffsetDateTime;

@Data
public class LuggageSearchRequestDto {
    private String location;        // Es: "Roma" o "Roma, Lazio"
    private OffsetDateTime dropOff; // ISO 8601
    private OffsetDateTime pickUp;  // ISO 8601
    private Integer bagsS;
    private Integer bagsM;
    private Integer bagsL;
    private Integer page;
    private Integer size;
}
