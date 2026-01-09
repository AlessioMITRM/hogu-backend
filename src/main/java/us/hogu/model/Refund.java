package us.hogu.model;

import java.time.OffsetDateTime;

import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "refunds")
public class Refund {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    private String refundId; // ID del rimborso dal gateway di pagamento
    private String paymentIntentId;
    private String captureId; // Per PayPal
    
    private Double amount;
    private String currency;
    
    @Enumerated(EnumType.ORDINAL)
    private RefundStatus status;
    
    private String reason;
    
    private OffsetDateTime requestedAt;
    private OffsetDateTime refundedAt;
    
    @ManyToOne
    @JoinColumn(name = "payment_id")
    private Payment payment;
}
