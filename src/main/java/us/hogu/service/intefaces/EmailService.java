package us.hogu.service.intefaces;

import us.hogu.common.constants.EmailConstants;

public interface EmailService {

	String generateOtp();

	void sendOtpEmail(String recipientEmail, String otp, String language);

	void sendEmail(String recipientEmail, EmailConstants emailConstants, String language);

	void sendEmailForRejectAccount(String recipientEmail, String motivation, String language);

	void sendOtpEmailForResetPassword(String recipientEmail, String otp, String language);

	void sendEmailForAccountActivation(String recipientEmail, String language);

	void sendEmailForAccountDeactivation(String recipientEmail, String language);

	void sendEmailForAccountBanned(String recipientEmail, String language);

	void sendEmailForAccountDeletion(String recipientEmail, String language);

}
