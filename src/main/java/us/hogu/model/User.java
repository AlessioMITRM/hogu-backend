package us.hogu.model;

import java.time.OffsetDateTime;
import java.util.List;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.Table;

import org.hibernate.annotations.CreationTimestamp;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import us.hogu.model.enums.UserRole;
import us.hogu.model.enums.UserStatus;


@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "user")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    private String name;
    
    private String surname;
    
    @Column(unique = true)
    private String email;
    
    private String passwordHash;
    
    @Enumerated(EnumType.ORDINAL)
    private UserRole role;
    
    @Enumerated(EnumType.ORDINAL)
    private UserStatus status;
    
    @CreationTimestamp
    private OffsetDateTime creationDate;
    
    private OffsetDateTime lastLogin;
    
    // Relazioni
    @OneToMany(mappedBy = "user")
    @ToString.Exclude
    private List<Booking> bookings;
    
    @OneToMany(mappedBy = "user")
    @ToString.Exclude
    private List<Review> reviews;
    
    @OneToMany(mappedBy = "user")
    @ToString.Exclude
    private List<Notification> notifications;
}
