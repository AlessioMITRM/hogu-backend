package us.hogu.service.intefaces;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;

import javax.management.ServiceNotFoundException;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

import us.hogu.controller.dto.request.RestaurantBookingRequestDto;
import us.hogu.controller.dto.request.RestaurantServiceRequestDto;
import us.hogu.controller.dto.response.RestaurantBookingResponseDto;
import us.hogu.controller.dto.response.RestaurantManagementResponseDto;
import us.hogu.controller.dto.response.ServiceDetailResponseDto;
import us.hogu.controller.dto.request.RestaurantAdvancedSearchRequestDto;
import us.hogu.controller.dto.request.RestaurantAvailabilityRequestDto;
import us.hogu.controller.dto.response.RestaurantAvailabilityResponseDto;
import us.hogu.controller.dto.response.ServiceSummaryResponseDto;
import us.hogu.repository.projection.RestaurantManagementProjection;

public interface RestaurantService {

	RestaurantBookingResponseDto createRestaurantBooking(RestaurantBookingRequestDto requestDto, Long userId);

	ServiceDetailResponseDto createRestaurant(Long providerId, RestaurantServiceRequestDto request,
			List<MultipartFile> images) throws IOException;
	
	void approveRestaurant(Long restaurantId) throws ServiceNotFoundException;

	List<RestaurantManagementResponseDto> getAllRestaurantsForAdmin();

	List<ServiceSummaryResponseDto> searchRestaurants(String searchTerm);

	ServiceDetailResponseDto updateRestaurant(Long providerId, Long restaurantId, RestaurantServiceRequestDto request,
			List<MultipartFile> images) throws IOException;

	Page<RestaurantManagementResponseDto> getProviderRestaurants(Long providerId, Pageable pageable);

	Page<RestaurantBookingResponseDto> getRestaurantBookings(Long restaurantId, Long providerId, Pageable pageable);

	Page<RestaurantBookingResponseDto> getUserRestaurantBookings(Long userId, Pageable pageable);

	Page<ServiceSummaryResponseDto> getActiveRestaurants(Pageable pageable);

	Page<ServiceSummaryResponseDto> advancedSearchRestaurants(RestaurantAdvancedSearchRequestDto searchRequest, Pageable pageable);

	RestaurantAvailabilityResponseDto checkRestaurantAvailability(Long restaurantId, RestaurantAvailabilityRequestDto availabilityRequest);

	ServiceDetailResponseDto getRestaurantDetail(Long restaurantId, LocalDate date, Integer numberOfPeople);

}
