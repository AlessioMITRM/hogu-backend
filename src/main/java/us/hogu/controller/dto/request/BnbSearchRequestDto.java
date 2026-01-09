package us.hogu.controller.dto.request;

import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat; // IMPORTANTE

import java.time.LocalDate;
import javax.validation.constraints.AssertTrue; // IMPORTANTE
import javax.validation.constraints.Min;
import javax.validation.constraints.Positive;

@Data
public class BnbSearchRequestDto {
    @Min(value = 0, message = "Page number must be non-negative")
    private int page = 0;

    @Positive(message = "Page size must be positive")
    private int size = 10;

    private String searchTerm;
    private String location;

    // --- AGGIUNGI @DateTimeFormat QUI ---
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) 
    private LocalDate checkIn;

    // --- AGGIUNGI @DateTimeFormat QUI ---
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) 
    private LocalDate checkOut;

    @Min(value = 1, message = "Number of adults must be at least 1")
    private int adults = 2;

    @Min(value = 0, message = "Number of children cannot be negative")
    private int children = 0;

    @Positive(message = "Number of rooms must be positive")
    private int rooms = 1;

    // --- AGGIUNGI @AssertTrue PER ATTIVARE LA VALIDAZIONE ---
    @AssertTrue(message = "Check-out date must be after check-in date")
    public boolean isValidDateRange() {
        // Se una delle due è null, non falliamo qui (ci penseranno altre validazioni se @NotNull è richiesto)
        // Se entrambe ci sono, controlliamo che checkout non sia prima di checkin
        return checkIn == null || checkOut == null || !checkOut.isBefore(checkIn);
    }
}