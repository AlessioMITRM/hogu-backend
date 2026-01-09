package us.hogu.model;

import java.time.OffsetDateTime;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.PositiveOrZero;
import javax.validation.constraints.Positive;

import org.hibernate.annotations.CreationTimestamp;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "applied_commissions")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AppliedCommission {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @NotNull(message = "La prenotazione è obbligatoria")
    @ManyToOne
    @JoinColumn(name = "booking_id", nullable = false)
    private Booking booking;
    
    @NotNull(message = "La commission setting è obbligatoria")
    @ManyToOne
    @JoinColumn(name = "commission_setting_id", nullable = false)
    private CommissionSetting commissionSetting;
    
    @NotNull(message = "L'importo della commissione è obbligatorio")
    @PositiveOrZero(message = "L'importo della commissione deve essere positivo o zero")
    private Double commissionAmount;
    
    @NotNull(message = "La percentuale della commissione applicata è obbligatoria")
    @PositiveOrZero(message = "La percentuale della commissione deve essere positiva o zero")
    private Double commissionRateApplied;
    
    @CreationTimestamp
    @NotNull(message = "La data di calcolo è obbligatoria")
    private OffsetDateTime calculatedAt;
}