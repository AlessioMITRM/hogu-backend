package us.hogu.service.intefaces;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import javax.validation.Valid;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

import us.hogu.configuration.security.dto.UserAccount;
import us.hogu.controller.dto.request.BnbRoomPriceRequestDto;
import us.hogu.controller.dto.request.BnbRoomRequestDto;
import us.hogu.controller.dto.request.BnbSearchRequestDto;
import us.hogu.controller.dto.request.BnbServiceRequestDto;
import us.hogu.controller.dto.response.BnbBookingResponseDto;
import us.hogu.controller.dto.response.BnbRoomResponseDto;
import us.hogu.controller.dto.response.BnbSearchResponseDto;
import us.hogu.controller.dto.response.BnbServiceResponseDto;

public interface BnbService {

	BnbSearchResponseDto searchBnbRooms(BnbSearchRequestDto searchRequest);

	List<BnbServiceResponseDto> getAllPublishedBnbServices();

	void addRoomPrice(UserAccount userAccount, Long roomId, BnbRoomPriceRequestDto dto);

	BnbBookingResponseDto createBooking(Long userId, Long roomId, LocalDate checkIn, LocalDate checkOut,
			Integer guests);

	BnbRoomResponseDto addRoomToService(UserAccount userAccount, Long bnbServiceId, BnbRoomRequestDto dto, List<MultipartFile> images);

	Optional<BnbServiceResponseDto> getBnbServiceById(Long id);

	Page<BnbBookingResponseDto> getBookingsForUser(Long userId, Pageable pageable);

	Page<BnbRoomResponseDto> getRoomsForService(Long bnbServiceId, Pageable pageable);

	Page<BnbBookingResponseDto> getBookingsForProvider(UserAccount userAccount, Long id, Pageable pageable);

	BnbServiceResponseDto createBnbService(UserAccount userAccount, @Valid BnbServiceRequestDto request, List<MultipartFile> images);

	Object updateBnbService(Long id, UserAccount userAccount, @Valid BnbServiceRequestDto request,
			List<MultipartFile> images);

	Object updateRoom(UserAccount userAccount, Long serviceId, Long roomId, @Valid BnbRoomRequestDto request,
			List<MultipartFile> images);

	Page<BnbServiceResponseDto> getAllBnbServicesByProvider(long accountId, Pageable pageable);

	BnbRoomResponseDto getRoomById(Long id, LocalDate checkIn, LocalDate checkOut);


}
