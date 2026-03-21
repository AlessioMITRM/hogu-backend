package us.hogu.service.intefaces;

import java.util.List;
import java.math.BigDecimal;

import javax.management.ServiceNotFoundException;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

import us.hogu.controller.dto.request.NccBookingRequestDto;
import us.hogu.controller.dto.request.NccSearchRequestDto;
import us.hogu.controller.dto.request.NccServiceRequestDto;
import us.hogu.controller.dto.response.InfoStatsDto;
import us.hogu.controller.dto.response.NccBookingResponseDto;
import us.hogu.controller.dto.response.NccBookingValidationResponseDto;
import us.hogu.controller.dto.response.NccDetailResponseDto;
import us.hogu.controller.dto.response.NccManagementResponseDto;
import us.hogu.controller.dto.response.ServiceDetailResponseDto;
import us.hogu.controller.dto.response.ServiceSummaryResponseDto;

public interface NccService {

	ServiceDetailResponseDto getNccServiceDetail(Long serviceId, String date, String time, Integer passengers, 
			String from, String fromCity, String fromProvince, String fromCountry,
			String to, String toCity, String toProvince, String toCountry,
			String tripType);

	NccBookingResponseDto createNccBooking(NccBookingRequestDto requestDto, Long userId);

    ServiceDetailResponseDto createNccService(Long providerId, NccServiceRequestDto requestDto,
            List<MultipartFile> images) throws Exception; 
    
	List<NccBookingResponseDto> getNccBookings(Long serviceId, Long providerId);

	void approveNccService(Long serviceId);

	List<NccManagementResponseDto> getAllNccServicesForAdmin();

	NccDetailResponseDto updateNccService(Long providerId, Long serviceId, NccServiceRequestDto requestDto,
			List<MultipartFile> images) throws Exception;

	Page<NccManagementResponseDto> getProviderNccServices(Long providerId, Pageable pageable);

	Page<NccBookingResponseDto> getUserNccBookings(Long userId, Pageable pageable);

	List<NccBookingResponseDto> getNccFullyPaidBookings(Long serviceId, Long providerId);

	NccBookingResponseDto getCurrentNccBooking(Long serviceId, Long providerId);
	
	Page<NccBookingResponseDto> getNccBookingsHistory(Long serviceId, Long providerId, Pageable pageable);

	NccBookingValidationResponseDto validateNccBookingByCode(Long providerId, String code);

	Page<ServiceSummaryResponseDto> getActiveNccServices(NccSearchRequestDto searchRequest, Pageable pageable);

	InfoStatsDto getInfo(Long providerId);

	NccDetailResponseDto getNccServiceByServiceIdAndProviderId(Long serviceId, Long providerId);

	void rectifyBooking(Long providerId, Long bookingId, BigDecimal newPrice, String note);

}
