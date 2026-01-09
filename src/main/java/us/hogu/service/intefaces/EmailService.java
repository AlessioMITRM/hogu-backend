package us.hogu.service.intefaces;

import us.hogu.common.constants.EmailConstants;

public interface EmailService {

	String generateOtp();

	void sendOtpEmail(String recipientEmail, String otp);

	void sendEmail(String recipientEmail, EmailConstants emailConstants);

	void sendEmailForRejectAccount(String recipientEmail, String motivation);

	void sendOtpEmailForResetPassword(String recipientEmail, String otp);

}
