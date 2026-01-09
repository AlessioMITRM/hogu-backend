package us.hogu.service.intefaces;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import us.hogu.controller.dto.request.BnbSearchRequestDto;
import us.hogu.controller.dto.response.BnbSearchResponseDto;

public interface BnbSearchService {
    /**
     * Performs an advanced search for BnB rooms based on multiple criteria
     * 
     * @param searchRequest The search request containing filtering parameters
     * @return Paginated search results
     */
    BnbSearchResponseDto searchBnbRooms(BnbSearchRequestDto searchRequest);

    /**
     * Checks room availability for specific dates and guest requirements
     * 
     * @param roomId The ID of the room to check
     * @param checkIn Check-in date
     * @param checkOut Check-out date
     * @param guests Total number of guests
     * @return Boolean indicating room availability
     */
    boolean checkRoomAvailability(Long roomId, java.time.LocalDate checkIn, java.time.LocalDate checkOut, int guests);
}
