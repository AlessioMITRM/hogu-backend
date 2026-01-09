package us.hogu.model;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToOne;
import javax.persistence.Table;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import us.hogu.model.enums.PaymentMethod;
import us.hogu.model.enums.PaymentStatus;


@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "payments")
public class Payment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @OneToOne
    @JoinColumn(name = "booking_id")
    private Booking booking;
    
    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;
    
    private Double amount;
    
    private String currency;
    
    private PaymentMethod paymentMethod; // STRIPE, PAYPAL
    
    private String paymentIdIntent;
    
    private PaymentStatus status; // IN_ATTESA, COMPLETED, FAILED
    
    private Double feeAmount;
    
    private Double netAmount;
    
    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private OffsetDateTime creationDate;

    @UpdateTimestamp
    @Column(nullable = false, updatable = true)
    private OffsetDateTime lastUpdateDate;
}
