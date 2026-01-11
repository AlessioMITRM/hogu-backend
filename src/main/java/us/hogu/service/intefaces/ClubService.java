package us.hogu.service.intefaces;

import java.util.List;

import javax.management.ServiceNotFoundException;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

import us.hogu.controller.dto.request.ClubBookingRequestDto;
import us.hogu.controller.dto.request.ClubServiceRequestDto;
import us.hogu.controller.dto.request.EventClubServiceRequestDto;
import us.hogu.controller.dto.request.EventCreateRequestDto;
import us.hogu.controller.dto.response.ClubBookingResponseDto;
import us.hogu.controller.dto.response.ClubManagementResponseDto;
import us.hogu.controller.dto.response.ClubServiceResponseDto;
import us.hogu.controller.dto.response.EventClubServiceResponseDto;
import us.hogu.controller.dto.response.ClubInfoStatsDto;
import us.hogu.controller.dto.response.ServiceDetailResponseDto;
import us.hogu.controller.dto.response.ServiceSummaryResponseDto;
import us.hogu.model.EventClubServiceEntity;
import us.hogu.repository.projection.ClubManagementProjection;

public interface ClubService {

	List<ServiceSummaryResponseDto> getActiveClubs(String searchText);

    ClubServiceResponseDto getClubDetail(Long clubId);
	
	List<ServiceSummaryResponseDto> getClubsWithEvents();

	ClubBookingResponseDto createClubBooking(ClubBookingRequestDto requestDto, Long userId);

	ServiceDetailResponseDto createClub(Long providerId, ClubServiceRequestDto requestDto, List<MultipartFile> images)
			throws Exception;

	ServiceDetailResponseDto updateClub(Long providerId, Long serviceId, ClubServiceRequestDto requestDto,
			List<MultipartFile> images) throws Exception;

	EventClubServiceResponseDto createEvent(Long providerId, Long serviceId, EventClubServiceRequestDto requestDto,
			List<MultipartFile> images) throws Exception;

	EventClubServiceResponseDto updateEvent(Long providerId, Long serviceId, EventClubServiceRequestDto requestDto,
			List<MultipartFile> images) throws Exception;

	EventClubServiceResponseDto getEvent(Long eventId);

	ClubManagementResponseDto getProviderClub(Long providerId, Long clubId);

	Page<ClubBookingResponseDto> getClubBookings(Long providerId, Long clubId, Pageable pageable);

	Page<ClubBookingResponseDto> getUserClubBookings(Long userId, Pageable pageable);

	Page<EventClubServiceResponseDto> getEvents(Long providerId, Long clubId, Pageable pageable);

	Page<EventClubServiceResponseDto> getEventsForPublic(Long clubId, Pageable pageable);

	Page<us.hogu.controller.dto.response.EventPublicResponseDto> getEventsForPublicWithFilters(
		String city, 
		String eventType, 
		String date, 
		Boolean table,
		Pageable pageable
	);

	Page<ClubBookingResponseDto> getClubBookingsPending(Long providerId, Long clubId, Pageable pageable);

	Page<EventClubServiceResponseDto> getEventsToday(Long providerId, Long clubId, Pageable pageable);

	ClubInfoStatsDto getInfo(Long providerId);

	EventClubServiceResponseDto getEventForProvider(Long providerId, Long eventId);

	EventClubServiceResponseDto createEvent(Long providerId, EventClubServiceRequestDto requestDto,
			List<MultipartFile> images) throws Exception;

}
