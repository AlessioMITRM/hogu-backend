package us.hogu.service.impl;

import org.apache.commons.lang3.RandomStringUtils;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import software.amazon.awssdk.services.ses.SesClient;
import software.amazon.awssdk.services.ses.model.Body;
import software.amazon.awssdk.services.ses.model.Content;
import software.amazon.awssdk.services.ses.model.Destination;
import software.amazon.awssdk.services.ses.model.Message;
import software.amazon.awssdk.services.ses.model.SendEmailRequest;
import software.amazon.awssdk.services.ses.model.SendEmailResponse;
import us.hogu.common.constants.EmailConstants;
import us.hogu.configuration.properties.AwsSesProperties;
import us.hogu.service.intefaces.EmailService;

@Service
@Transactional
@RequiredArgsConstructor
public class EmailServiceImpl implements EmailService {
    private final AwsSesProperties awsSesProperties;
    private final SesClient sesClient;

    
    @Override
    public String generateOtp() {
        return RandomStringUtils.randomNumeric(6);
    }

    
    @Override
    public void sendOtpEmailForResetPassword(String recipientEmail, String otp) {
        EmailConstants emailTemplate = EmailConstants.PASSWORD_RESET;

        Destination destination = Destination.builder()
                .toAddresses(recipientEmail)
                .build();

        Content subject = Content.builder()
                .data(emailTemplate.getObject())
                .build();

        Content textBody = Content.builder()
                .data(String.format(emailTemplate.getTextBody(), otp))
                .build();

        Body body = Body.builder()
                .text(textBody)
                .build();

        Message message = Message.builder()
                .subject(subject)
                .body(body)
                .build();

        SendEmailRequest emailRequest = SendEmailRequest.builder()
                .destination(destination)
                .message(message)
                .source(awsSesProperties.getSenderEmail())
                .build();

        //sesClient.sendEmail(emailRequest);
    }
    
    @Override
    public void sendOtpEmail(String recipientEmail, String otp) {
        EmailConstants emailTemplate = EmailConstants.OTP_VERIFICATION;

        Destination destination = Destination.builder()
                .toAddresses(recipientEmail)
                .build();

        Content subject = Content.builder()
                .data(emailTemplate.getObject())
                .build();

        Content textBody = Content.builder()
                .data(String.format(emailTemplate.getTextBody(), otp))
                .build();

        Body body = Body.builder()
                .text(textBody)
                .build();

        Message message = Message.builder()
                .subject(subject)
                .body(body)
                .build();

        SendEmailRequest emailRequest = SendEmailRequest.builder()
                .destination(destination)
                .message(message)
                .source(awsSesProperties.getSenderEmail())
                .build();

        //sesClient.sendEmail(emailRequest);
    }
    
    @Override
    public void sendEmailForRejectAccount(String recipientEmail, String motivation) {
        EmailConstants emailTemplate = EmailConstants.ACCOUNT_REJECTION;

        Destination destination = Destination.builder()
                .toAddresses(recipientEmail)
                .build();

        Content subject = Content.builder()
                .data(emailTemplate.getObject())
                .build();

        Content textBody = Content.builder()
                .data(String.format(emailTemplate.getTextBody(), motivation))
                .build();

        Body body = Body.builder()
                .text(textBody)
                .build();

        Message message = Message.builder()
                .subject(subject)
                .body(body)
                .build();

        SendEmailRequest emailRequest = SendEmailRequest.builder()
                .destination(destination)
                .message(message)
                .source(awsSesProperties.getSenderEmail())
                .build();

        //sesClient.sendEmail(emailRequest);
    }
    
    @Override
    public void sendEmail(String recipientEmail, EmailConstants emailConstants) {
        Destination destination = Destination.builder()
                .toAddresses(recipientEmail)
                .build();

        Content subject = Content.builder()
                .data(emailConstants.getObject())
                .build();

        Content textBody = Content.builder()
                .data(emailConstants.getTextBody())
                .build();

        Body body = Body.builder()
                .text(textBody)
                .build();

        Message message = Message.builder()
                .subject(subject)
                .body(body)
                .build();

        SendEmailRequest emailRequest = SendEmailRequest.builder()
                .destination(destination)
                .message(message)
                .source(awsSesProperties.getSenderEmail())
                .build();

        //SendEmailResponse response = sesClient.sendEmail(emailRequest);
    }

}
