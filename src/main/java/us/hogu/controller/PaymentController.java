package us.hogu.controller;

import javax.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import us.hogu.client.feign.dto.request.PayPalPaymentRequestDto;
import us.hogu.client.feign.dto.request.StripePaymentRequestDto;
import us.hogu.configuration.security.dto.UserAccount;
import us.hogu.client.feign.dto.response.PaymentResponseDto;
import us.hogu.service.intefaces.PaymentService;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {
    private final PaymentService paymentService;

    @PostMapping("/stripe")
    @PreAuthorize("hasRole('CLIENTE')")
    public ResponseEntity<PaymentResponseDto> processStripePayment(
            @AuthenticationPrincipal UserAccount userAccount,
            @Valid @RequestBody StripePaymentRequestDto requestDto) {
        PaymentResponseDto response = paymentService.processStripePayment(requestDto, userAccount.getAccountId());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/paypal")
    @PreAuthorize("hasRole('CLIENTE')")
    public ResponseEntity<PaymentResponseDto> processPayPalPayment(
            @AuthenticationPrincipal UserAccount userAccount,
            @Valid @RequestBody PayPalPaymentRequestDto requestDto) {
        PaymentResponseDto response = paymentService.processPayPalPayment(requestDto, userAccount.getAccountId());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/paypal/execute")
    @PreAuthorize("hasRole('CLIENTE')")
    public ResponseEntity<PaymentResponseDto> executePayPalPayment(
            @AuthenticationPrincipal UserAccount userAccount,
            @RequestParam String paymentId,
            @RequestParam String payerId) {
        PaymentResponseDto response = paymentService.executePayPalPayment(paymentId, payerId, userAccount.getAccountId());
        return ResponseEntity.ok(response);
    }
}