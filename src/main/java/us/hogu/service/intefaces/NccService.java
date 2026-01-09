package us.hogu.service.intefaces;

import java.util.List;

import javax.management.ServiceNotFoundException;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

import us.hogu.controller.dto.request.NccBookingRequestDto;
import us.hogu.controller.dto.request.NccSearchRequestDto;
import us.hogu.controller.dto.request.NccServiceRequestDto;
import us.hogu.controller.dto.request.RestaurantBookingRequestDto;
import us.hogu.controller.dto.response.NccBookingResponseDto;
import us.hogu.controller.dto.response.NccManagementResponseDto;
import us.hogu.controller.dto.response.ServiceDetailResponseDto;
import us.hogu.controller.dto.response.ServiceSummaryResponseDto;
import us.hogu.model.RestaurantBooking;
import us.hogu.model.RestaurantServiceEntity;
import us.hogu.model.User;
import us.hogu.repository.projection.NccManagementProjection;

public interface NccService {

	ServiceDetailResponseDto getNccServiceDetail(Long serviceId);

	NccBookingResponseDto createNccBooking(NccBookingRequestDto requestDto, Long userId);

    ServiceDetailResponseDto createNccService(Long providerId, NccServiceRequestDto requestDto,
            List<MultipartFile> images) throws Exception; 
    
	List<NccBookingResponseDto> getNccBookings(Long serviceId, Long providerId);

	void approveNccService(Long serviceId);

	List<NccManagementResponseDto> getAllNccServicesForAdmin();

	ServiceDetailResponseDto updateNccService(Long providerId, Long serviceId, NccServiceRequestDto requestDto,
			List<MultipartFile> images) throws Exception;

	Page<NccManagementResponseDto> getProviderNccServices(Long providerId, Pageable pageable);

	Page<NccBookingResponseDto> getUserNccBookings(Long userId, Pageable pageable);

	Page<ServiceSummaryResponseDto> getActiveNccServices(NccSearchRequestDto searchRequest, Pageable pageable);

	Page<ServiceSummaryResponseDto> searchNccServices(String searchTerm, Pageable pageable);

}
