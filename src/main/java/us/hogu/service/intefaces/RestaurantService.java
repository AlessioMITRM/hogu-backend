package us.hogu.service.intefaces;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;

import javax.management.ServiceNotFoundException;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

import us.hogu.controller.dto.response.InfoStatsDto;
import us.hogu.controller.dto.request.RestaurantBookingRequestDto;
import us.hogu.controller.dto.request.RestaurantServiceRequestDto;
import us.hogu.controller.dto.response.RestaurantBookingResponseDto;
import us.hogu.controller.dto.response.RestaurantManagementResponseDto;
import us.hogu.controller.dto.response.RestaurantServiceDetailResponseDto;
import us.hogu.controller.dto.response.ServiceDetailResponseDto;
import us.hogu.controller.dto.request.RestaurantAdvancedSearchRequestDto;
import us.hogu.controller.dto.request.RestaurantAvailabilityRequestDto;
import us.hogu.controller.dto.response.RestaurantAvailabilityResponseDto;
import us.hogu.controller.dto.response.ServiceSummaryResponseDto;
import us.hogu.controller.dto.response.RestaurantBookingValidationResponseDto;
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

	Page<RestaurantBookingResponseDto> getRestaurantBookingsPending(Long restaurantId, Long providerId, Pageable pageable);

	Page<RestaurantBookingResponseDto> getRestaurantBookingsHistory(Long restaurantId, Long providerId, Pageable pageable);
	
	Page<RestaurantBookingResponseDto> getRestaurantBookingsUpcoming(Long restaurantId, Long providerId, Pageable pageable);

	void acceptBooking(Long providerId, Long bookingId);

	void cancelBooking(Long providerId, Long bookingId, String reason);

	Page<RestaurantBookingResponseDto> getUserRestaurantBookings(Long userId, Pageable pageable);

	Page<ServiceSummaryResponseDto> getActiveRestaurants(Pageable pageable);

	Page<ServiceSummaryResponseDto> advancedSearchRestaurants(RestaurantAdvancedSearchRequestDto searchRequest, Pageable pageable);

	RestaurantAvailabilityResponseDto checkRestaurantAvailability(Long restaurantId, RestaurantAvailabilityRequestDto availabilityRequest);

	ServiceDetailResponseDto getRestaurantDetail(Long restaurantId, LocalDate date, Integer numberOfPeople);
	
	InfoStatsDto getInfo(Long providerId);

	RestaurantServiceDetailResponseDto getRestaurantServiceByIdAndProvider(Long serviceId, Long providerId);

    Page<RestaurantBookingResponseDto> getCompletedBookingsForCommissions(Long providerId, Pageable pageable);

    us.hogu.client.feign.dto.response.PaymentResponseDto payRestaurantCommissions(Long providerId, String returnUrl, String cancelUrl);

    us.hogu.client.feign.dto.response.PaymentResponseDto executeRestaurantCommissionPayment(Long providerId, String paymentId, String payerId);

    RestaurantBookingValidationResponseDto validateBookingByCode(Long providerId, String code);

    us.hogu.client.feign.dto.response.PaymentResponseDto payRestaurantCommissionsStripe(Long providerId, String returnUrl, String cancelUrl);

    us.hogu.client.feign.dto.response.PaymentResponseDto executeRestaurantCommissionPaymentStripe(Long providerId, String paymentId);
}
