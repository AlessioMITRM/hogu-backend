package us.hogu.controller.customer;

import javax.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
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
import us.hogu.controller.dto.response.BookingInfoDTO;
import us.hogu.client.feign.dto.response.PaymentResponseDto;
import us.hogu.model.enums.ServiceType;
import us.hogu.service.intefaces.PaymentService;

@RestController
@RequestMapping("/api/customer/payment")
@PreAuthorize("hasAnyRole(T(us.hogu.model.enums.UserRole).CUSTOMER.name())")
@RequiredArgsConstructor
public class PaymentCustomerController {
    private final PaymentService paymentService;

    
    @GetMapping("/paypal/booking-info")
    public ResponseEntity<BookingInfoDTO> getBookingInfoByPaymentId(
            @AuthenticationPrincipal UserAccount userAccount,
            @RequestParam String paymentId) {
        BookingInfoDTO info = paymentService.getBookingInfoByPaymentId(paymentId, userAccount.getAccountId());
        return ResponseEntity.ok(info);
    }

    @Operation(summary = "Recupera la prenotazione in attesa di pagamento", 
               description = "Restituisce l'ultima prenotazione dell'utente con stato WAITING_CUSTOMER_PAYMENT o PENDING.")
    @GetMapping("/booking/pending-payment")
    public ResponseEntity<BookingInfoDTO> getPendingPaymentBooking(
            @AuthenticationPrincipal UserAccount userAccount) {
        
        BookingInfoDTO bookingInfo = paymentService.getPendingBooking(userAccount.getAccountId());
        return ResponseEntity.ok(bookingInfo);
    }

    @DeleteMapping("/booking/{bookingId}/cancel")
    public ResponseEntity<Void> cancelBooking(
            @AuthenticationPrincipal UserAccount userAccount,
            @PathVariable Long bookingId,
            @RequestParam ServiceType serviceType) {
        paymentService.cancelBooking(bookingId, serviceType, userAccount.getAccountId());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/stripe")
    public ResponseEntity<PaymentResponseDto> processStripePayment(
            @AuthenticationPrincipal UserAccount userAccount,
            @Valid @RequestBody StripePaymentRequestDto requestDto) {
        PaymentResponseDto response = paymentService.processStripePayment(requestDto, userAccount.getAccountId());
        return ResponseEntity.ok(response);
    }
 
    @PostMapping("/paypal")
    public ResponseEntity<PaymentResponseDto> processPayPalPayment(
            @AuthenticationPrincipal UserAccount userAccount,
            @Valid @RequestBody PayPalPaymentRequestDto requestDto) {
        PaymentResponseDto response = paymentService.processPayPalPayment(requestDto, userAccount.getAccountId());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/paypal/execute")
    public ResponseEntity<PaymentResponseDto> executePayPalPayment(
            @AuthenticationPrincipal UserAccount userAccount,
            @RequestParam String paymentId,
            @RequestParam String payerId) {
        PaymentResponseDto response = paymentService.executePayPalPayment(paymentId, payerId, userAccount.getAccountId());
        return ResponseEntity.ok(response);
    }

}