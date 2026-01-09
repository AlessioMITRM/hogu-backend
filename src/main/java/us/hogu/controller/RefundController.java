package us.hogu.controller;

import java.util.List;

import javax.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.stripe.service.RefundService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import us.hogu.configuration.security.dto.UserAccount;
import us.hogu.controller.dto.request.RefundRequestDto;
import us.hogu.controller.dto.response.RefundResponseDto;
import us.hogu.service.intefaces.PaymentService;

/*@RestController
@RequestMapping("/api/refunds")
@RequiredArgsConstructor
public class RefundController {
    private final PaymentService paymentService;

    
    @PostMapping("/refunds/request")
    @PreAuthorize("hasRole('CLIENTE')")
    @Operation(summary = "Richiedi rimborso", description = "Richiede un rimborso per un pagamento")
    public ResponseEntity<Void> requestRefund(
            @Parameter(hidden = true)
            @AuthenticationPrincipal UserAccount userAccount,
            @Valid @RequestBody RefundRequestDto requestDto) {
        
        paymentService.requestRefund(
            requestDto.getPaymentId(), 
            userAccount.getIdAccount(), 
            requestDto.getReason()
        );
        
        return ResponseEntity.accepted().build();
    }

    @GetMapping("/refunds/my-refunds")
    @PreAuthorize("hasRole('CLIENTE')")
    @Operation(summary = "I miei rimborsi", description = "Restituisce la cronologia dei rimborsi")
    public ResponseEntity<List<RefundResponseDto>> getUserRefunds(
            @Parameter(hidden = true)
            @AuthenticationPrincipal UserAccount userAccount) {
        // Questo richiederebbe un metodo aggiuntivo in PaymentService
        List<RefundResponseDto> response = paymentService.getUserRefunds(userAccount.getIdAccount());
        return ResponseEntity.ok(response);
    }
}*/
