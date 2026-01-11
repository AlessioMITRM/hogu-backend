package us.hogu.model;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Column;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.PastOrPresent;

import org.hibernate.annotations.CreationTimestamp;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import us.hogu.model.enums.BookingStatus;
import us.hogu.model.enums.ServiceType;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Inheritance(strategy = InheritanceType.JOINED)
public abstract class Booking {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @NotNull(message = "L'utente è obbligatorio")
    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    
    @NotNull(message = "Il tipo di servizio è obbligatorio")
    @Enumerated(EnumType.ORDINAL)
    @Column(nullable = false)
    private ServiceType serviceType;
    
    @NotNull(message = "L'ID del servizio è obbligatorio")
    @Column(name = "service_id", nullable = false)
    private Long serviceId;
        
    @NotNull(message = "Lo stato della prenotazione è obbligatorio")
    @Enumerated(EnumType.ORDINAL)
    @Column(nullable = false, length = 50)
    private BookingStatus status;
    
    @NotNull(message = "L'importo totale è obbligatorio")
    @Column(nullable = false)
    private BigDecimal totalAmount;
    
    private String billingFirstName;
    
    private String billingLastName;
    
    private String billingCompanyName;
    
    @NotNull(message = "Indirizzo della fatturazione obbligatorio")
    @Column(nullable = false)
    private String billingAddress;
    
    private String billingTaxCode;     // Codice fiscale
    
    private String billingVatNumber;   // Partita IVA
    
    @NotNull(message = "Email Della fattura obbligatoria")
    @Column(nullable = false)
    private String billingEmail;
    
    private boolean invoiceProcessed;  // Fattura elaborata
    
    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private OffsetDateTime creationDate;
}