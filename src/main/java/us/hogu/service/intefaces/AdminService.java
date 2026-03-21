package us.hogu.service.intefaces;

import java.util.List;

import org.springframework.data.domain.Page;

import us.hogu.controller.dto.response.AdminCustomerDetailResponseDto;
import us.hogu.controller.dto.response.AdminCustomerResponseDto;
import us.hogu.controller.dto.response.AdminDashboardKpiResponseDto;
import us.hogu.controller.dto.response.AdminProviderResponseDto;
import us.hogu.controller.dto.response.AdminBookingDetailResponseDto;
import us.hogu.controller.dto.response.AdminBookingResponseDto;
import us.hogu.controller.dto.response.UserDocumentResponseDto;
import us.hogu.model.enums.BookingStatus;
import us.hogu.model.enums.UserStatus;

public interface AdminService {

	List<us.hogu.controller.dto.response.PendingVerificationResponseDto> getPendingVerifications();

	void approveServiceVerification(Long verificationId);

	void rejectServiceVerification(Long verificationId, String motivation);

	Page<AdminCustomerResponseDto> getCustomers(String search, int page, int size);

	Page<AdminProviderResponseDto> getProviders(String search, int page, int size);

	Page<AdminBookingResponseDto> getBookings(String search, int page, int size);

	void updateBookingStatus(Long bookingId, BookingStatus status, String reason);

	AdminBookingDetailResponseDto getBookingDetail(Long bookingId);

	UserDocumentResponseDto getFileUserDocument(Long idDocument);

	AdminDashboardKpiResponseDto getDashboardKpis();

	void updateUserStatus(Long userId, UserStatus status);
	
	List<UserDocumentResponseDto> getProviderDocuments(Long providerId);

	AdminCustomerDetailResponseDto getCustomerDetail(Long userId);
}
