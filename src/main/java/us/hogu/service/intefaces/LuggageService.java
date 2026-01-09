package us.hogu.service.intefaces;

import java.io.IOException;
import java.util.List;

import javax.management.ServiceNotFoundException;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

import us.hogu.controller.dto.request.LuggageBookingRequestDto;
import us.hogu.controller.dto.request.LuggageSearchRequestDto;
import us.hogu.controller.dto.request.LuggageServiceRequestDto;
import us.hogu.controller.dto.response.LuggageBookingResponseDto;
import us.hogu.controller.dto.response.LuggageSearchResultResponseDto;
import us.hogu.controller.dto.response.LuggageServiceAdminResponseDto;
import us.hogu.controller.dto.response.LuggageServiceDetailResponseDto;
import us.hogu.controller.dto.response.LuggageServiceProviderResponseDto;
import us.hogu.controller.dto.response.LuggageServiceResponseDto;
import us.hogu.controller.dto.response.ServiceDetailResponseDto;
import us.hogu.controller.dto.response.ServiceSummaryResponseDto;
import us.hogu.repository.projection.LuggageManagementProjection;

public interface LuggageService {

	LuggageServiceResponseDto getLuggageServiceDetail(Long serviceId) ;

	LuggageBookingResponseDto createLuggageBooking(Long userId, LuggageBookingRequestDto requestDto);

	List<LuggageServiceProviderResponseDto> getProviderLuggageServices(Long providerId);

	void approveLuggageService(Long serviceId) ;

	List<LuggageServiceAdminResponseDto> getAllLuggageServicesForAdmin();

	Page<ServiceSummaryResponseDto> getAllLuggageServicesByProvider(Long providerId, Pageable pageable);

	Page<LuggageBookingResponseDto> getLuggageBookings(Long serviceId, Long providerId, Pageable pageable);

	Page<LuggageBookingResponseDto> getUserLuggageBookings(Long userId, Pageable pageable);

	Page<ServiceSummaryResponseDto> searchLuggageServices(String searchTerm, Pageable pageable);

	Page<ServiceSummaryResponseDto> getAllActiveLuggageServices(Pageable pageable);

	Page<LuggageSearchResultResponseDto> searchNative(LuggageSearchRequestDto request, Pageable pageable);

	LuggageServiceDetailResponseDto createLuggageService(Long providerId, LuggageServiceRequestDto requestDto,
			List<MultipartFile> images) throws IOException;

	LuggageServiceDetailResponseDto updateLuggageService(Long providerId, Long serviceId, LuggageServiceRequestDto requestDto,
			List<MultipartFile> images) throws Exception;

	LuggageServiceProviderResponseDto getLuggageServiceByIdAndProvider(Long serviceId, Long providerId);

	List<LuggageServiceProviderResponseDto> getLuggageServicesByProviderId(Long providerId);


}
