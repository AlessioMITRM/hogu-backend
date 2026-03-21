package us.hogu.controller.publicapi;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import us.hogu.exception.ValidationException;
import us.hogu.service.intefaces.StripeService;

@Slf4j
@RestController
@RequestMapping("/api/public/stripe")
@RequiredArgsConstructor
@Tag(name = "Stripe Webhook", description = "Endpoints per i Webhook di Stripe")
public class StripeWebhookController {

    private final StripeService stripeService;

    @PostMapping("/webhook")
    @Operation(summary = "Gestione eventi Webhook Stripe", description = "Riceve e processa le notifiche asincrone da Stripe (es. pagamento completato)")
    public ResponseEntity<String> handleStripeWebhook(
            @RequestBody String payload,
            @RequestHeader("Stripe-Signature") String sigHeader) {

        log.info("Ricevuto Webhook da Stripe");

        try {
            boolean processed = stripeService.handleWebhook(payload, sigHeader);

            if (processed) {
                return ResponseEntity.ok("OK");
            }
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Errore Webhook");
        } catch (ValidationException e) {
            log.warn("Webhook Stripe rifiutato: {}", e.getMessage());
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            log.error("Errore durante l'elaborazione del Webhook Stripe", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Errore Webhook");
        }
    }
}
