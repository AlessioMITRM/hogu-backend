package us.hogu.service.intefaces;

import java.util.List;
import us.hogu.controller.dto.response.PriceChangeRequestDto;

public interface CustomerService {
    List<PriceChangeRequestDto> getUpcomingBookings(Long customerId, int page, int size);

    List<PriceChangeRequestDto> getPastBookings(Long customerId, int page, int size);
}
