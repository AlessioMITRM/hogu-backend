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
import us.hogu.controller.dto.response.BnbServiceDetailResponseDto;
import us.hogu.controller.dto.response.BnbServiceResponseDto;
import us.hogu.controller.dto.response.InfoStatsDto;

import us.hogu.controller.dto.request.BnbBookingRequestDto;

public interface BnbService {

	BnbSearchResponseDto searchBnbRooms(BnbSearchRequestDto searchRequest);

	List<BnbServiceResponseDto> getAllPublishedBnbServices();

	void addRoomPrice(UserAccount userAccount, Long roomId, BnbRoomPriceRequestDto dto);

	BnbBookingResponseDto createBooking(Long userId, Long roomId, LocalDate checkIn, LocalDate checkOut,
			Integer guests);

	BnbBookingResponseDto createBooking(BnbBookingRequestDto request, Long userId);

	BnbRoomResponseDto addRoomToService(UserAccount userAccount, Long bnbServiceId, BnbRoomRequestDto dto,
			List<MultipartFile> images) throws Exception;

	Optional<BnbServiceResponseDto> getBnbServiceById(Long id);

	Page<BnbBookingResponseDto> getBookingsForUser(Long userId, Pageable pageable);

	Page<BnbRoomResponseDto> getRoomsForService(Long bnbServiceId, Pageable pageable);

	Page<BnbRoomResponseDto> getRoomsForServiceByProvider(Long providerId, Pageable pageable);

	Page<BnbBookingResponseDto> getBookingsForProvider(UserAccount userAccount, Long id, Pageable pageable);

	Page<BnbBookingResponseDto> getTodayBookingsForProvider(UserAccount userAccount, Long id, Pageable pageable);

	Page<BnbBookingResponseDto> getUpcomingBookingsForProvider(UserAccount userAccount, Long id, Pageable pageable);

	Page<BnbBookingResponseDto> getHistoryBookingsForProvider(UserAccount userAccount, Long id, Pageable pageable);

	BnbServiceResponseDto createBnbService(UserAccount userAccount, @Valid BnbServiceRequestDto request,
			List<MultipartFile> images) throws IOException;

	BnbServiceDetailResponseDto updateBnbService(Long id, UserAccount userAccount, @Valid BnbServiceRequestDto request,
			List<MultipartFile> images) throws Exception;

	Object updateRoom(UserAccount userAccount, Long serviceId, Long roomId, @Valid BnbRoomRequestDto request,
			List<MultipartFile> images) throws Exception;

	Page<BnbServiceResponseDto> getAllBnbServicesByProvider(long accountId, Pageable pageable);

	BnbRoomResponseDto getRoomById(Long id, LocalDate checkIn, LocalDate checkOut);

	BnbRoomResponseDto getRoomByIdForProvider(Long id, Long providerId);

	InfoStatsDto getInfo(Long providerId);

	BnbServiceDetailResponseDto getBnbServiceByIdAndProvider(Long serviceId, Long providerId);

	us.hogu.controller.dto.response.BnbBookingValidationResponseDto validateBnbBookingByCode(Long providerId,
			String code);

}
