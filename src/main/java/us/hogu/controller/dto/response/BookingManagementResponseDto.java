package us.hogu.controller.dto.response;

import java.util.List;

import us.hogu.controller.dto.common.PaginationInfo;
import lombok.Data;

@Data
public class BookingManagementResponseDto {
	private List<BookingDetailResponseDto> bookings;
	
	private PaginationInfo pagination;
}
