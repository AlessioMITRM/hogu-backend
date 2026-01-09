package us.hogu.model;

import java.time.OffsetDateTime;
import javax.persistence.*;

import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "user_document")
public class UserDocument {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String filename;

    @Lob
    @Column(nullable = false)
    private byte[] fileData;

    // Stato di validazione
    private boolean approved;

    // Relazione con la verifica del servizio
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_service_verification_id", nullable = false)
    private UserServiceVerification userServiceVerification;

	@CreationTimestamp
	@Column(nullable = false, updatable = false)
	private OffsetDateTime creationDate;
}