package us.hogu.model;

import java.time.OffsetDateTime;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
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
import us.hogu.model.enums.VerificationStatusServiceEY;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "user_service_verification")
public class UserServiceVerification {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne
	@JoinColumn(name = "user_id", nullable = false)
	private User user;

	// Tipo di servizio che l'utente può offrire
	@Enumerated(EnumType.ORDINAL)
	@Column(nullable = false)
	private ServiceType serviceType;

	// Flag di verifica documenti richiesti
	private boolean licenseValid; // ad esempio per NCC o altri servizi regolamentati

	private boolean vatValid; // Partita iva valida

	// Stato di approvazione dell’admin per questo servizio
	@Enumerated(EnumType.ORDINAL)
	private VerificationStatusServiceEY verificationStatus;

	@CreationTimestamp
	@Column(nullable = false, updatable = false)
	private OffsetDateTime creationDate;

	@UpdateTimestamp
	@Column(nullable = false, updatable = true)
	private OffsetDateTime lastUpdateDate;
}
