package us.hogu.controller.dto.request;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class EventSearchRequestDto {
    private String location;
    private String eventType;
    private Boolean table;
    private String date; // Formato stringa (es. ISO-8601)
}