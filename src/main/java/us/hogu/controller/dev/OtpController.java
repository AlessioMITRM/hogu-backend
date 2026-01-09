package us.hogu.controller.dev;

import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;
import us.hogu.service.impl.EmailServiceImpl;

@Profile({"dev"})
@RestController
@RequestMapping("/otp")
@RequiredArgsConstructor
public class OtpController {

    private final EmailServiceImpl otpEmailService;

    @GetMapping("/send")
    public String sendOtp(@RequestParam String email) {
        String otp = otpEmailService.generateOtp();
        otpEmailService.sendOtpEmail(email, otp);
        return "OTP inviata a " + email;
    }
}

