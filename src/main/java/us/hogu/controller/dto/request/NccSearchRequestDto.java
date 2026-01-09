package us.hogu.controller.dto.request;

import lombok.Data;

@Data
public class NccSearchRequestDto {
    private String departureLocation;       // e.g. "Rome, Rome, Lazio"
    private String departureAddress;        // e.g. "Via dei santi 21"

    private String destinationLocation;     // e.g. "Rome, Rome, Lazio"
    private String destinationAddress;      // e.g. "Via dei remi"

    private String departureDate;           // format: dd/MM/yyyy (e.g. "04/12/2025")
    private String departureTime;           // format: HH:mm (e.g. "23:00")

    private Integer passengers;             // e.g. 1
}
