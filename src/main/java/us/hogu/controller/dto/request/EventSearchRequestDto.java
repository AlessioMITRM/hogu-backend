package us.hogu.controller.dto.request;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class EventSearchRequestDto {
    private ServiceLocaleRequestDto locale; // Changed to Object
    // private ServiceLocaleRequestDto locale; // Removed duplicate
    private String eventType;
    private Boolean table;
    private String date; // Formato stringa (es. ISO-8601)
}