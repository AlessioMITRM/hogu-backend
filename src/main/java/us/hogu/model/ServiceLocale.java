package us.hogu.model;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import us.hogu.model.enums.ServiceType;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "service_locales")
public class ServiceLocale {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull(message = "Il tipo di servizio è obbligatorio")
    @Enumerated(EnumType.ORDINAL)
    @Column(nullable = false)
    private ServiceType serviceType;

    @Column(nullable = false, length = 5)
    private String language; 

    @Column(columnDefinition = "TEXT")
    private String country;
    
    @Column(columnDefinition = "TEXT")
    private String state;
    
    @Column(columnDefinition = "TEXT")
    private String city;
    
    @Column(columnDefinition = "TEXT")
    private String address;
}