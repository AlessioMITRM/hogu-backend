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
import javax.persistence.Table;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import us.hogu.model.enums.ServiceType;

@Entity
@Table(name = "commission_settings")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CommissionSetting {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    private ServiceType serviceType;
    
    private Double commissionRate;
    
    private Double minCommissionAmount;
    
    private OffsetDateTime effectiveFrom;
    
    private OffsetDateTime effectiveTo;
    
    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private OffsetDateTime creationDate;

    @UpdateTimestamp
    @Column(nullable = false, updatable = true)
    private OffsetDateTime lastUpdateDate;
    
    @ManyToOne
    @JoinColumn(name = "created_by_admin_id")
    private User admin;
}
