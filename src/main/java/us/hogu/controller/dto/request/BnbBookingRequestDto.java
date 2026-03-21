package us.hogu.controller.dto.request;

import java.time.LocalDate;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

import lombok.Data;

@Data
public class BnbBookingRequestDto {
    @NotNull
    @Min(1)
    private Long bnbServiceId;
    
    @NotNull
    @Min(1)
    private Long roomId;
    
    @NotNull
    private LocalDate checkInDate;
    
    @NotNull
    private LocalDate checkOutDate;
    
    @NotNull
    private Integer numberOfGuests;    
    
    private String billingFirstName;
    
    private String billingLastName;
    
    @NotBlank    
    private String billingAddress;
    
    @NotBlank
    private String billingEmail;
    
    private String fiscalCode;
    
    private String taxId;
}
