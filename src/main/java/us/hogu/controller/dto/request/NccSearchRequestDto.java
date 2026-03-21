package us.hogu.controller.dto.request;

import lombok.Data;

@Data
public class NccSearchRequestDto {
        
    private ServiceLocaleRequestDto departureAddress;

    private ServiceLocaleRequestDto destinationAddress;

    private String departureDate;           // format: dd/MM/yyyy (e.g. "04/12/2025")
    private String departureTime;           // format: HH:mm (e.g. "23:00")

    private Integer passengers;             // e.g. 1
}
