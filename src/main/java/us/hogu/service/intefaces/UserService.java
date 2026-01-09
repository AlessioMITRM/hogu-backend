package us.hogu.service.intefaces;

import java.util.List;

import us.hogu.controller.dto.request.OtpVerificationRequestDto;
import us.hogu.controller.dto.request.PasswordResetConfirmDto;
import us.hogu.controller.dto.request.PasswordResetDashboard;
import us.hogu.controller.dto.request.PasswordResetRequestDto;
import us.hogu.controller.dto.request.ProviderRegistrationRequestDto;
import us.hogu.controller.dto.request.UserLoginRequestDto;
import us.hogu.configuration.security.dto.UserAccount;
import us.hogu.controller.dto.request.CustomerRegistrationRequestDto;
import us.hogu.controller.dto.request.UserUpdateRequestDto;
import us.hogu.controller.dto.response.AuthResponseDto;
import us.hogu.controller.dto.response.UserProfileResponseDto;
import us.hogu.controller.dto.response.UserResponseDto;
import us.hogu.model.enums.UserRole;
import us.hogu.repository.projection.UserSummaryProjection;

public interface UserService {

	AuthResponseDto customerRegistration(CustomerRegistrationRequestDto requestDto);

	AuthResponseDto login(UserLoginRequestDto requestDto);

	UserResponseDto getUserProfile(Long userId);

	List<UserSummaryProjection> getUsersByRole(UserRole role);

	UserProfileResponseDto updateUserProfile(Long userId, UserUpdateRequestDto requestDto);

	AuthResponseDto verificateOtpCustomer(OtpVerificationRequestDto request);

	AuthResponseDto providerRegistration(ProviderRegistrationRequestDto request) throws Exception;

	void resendOtpVerification(String email);

	AuthResponseDto verificateOtpProvider(OtpVerificationRequestDto request);

	void confirmPasswordReset(PasswordResetConfirmDto confirmDto);

	void requestPasswordReset(PasswordResetRequestDto requestDto);

	void passwordResetDashboard(UserAccount userAccount, PasswordResetDashboard requestDto);

}
