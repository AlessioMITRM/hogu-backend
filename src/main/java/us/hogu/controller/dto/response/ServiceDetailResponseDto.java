package us.hogu.controller.dto.response;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

import javax.validation.constraints.NotBlank;

import io.micrometer.core.lang.NonNull;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import lombok.experimental.SuperBuilder;
import us.hogu.model.enums.ServiceType;

@Data
@SuperBuilder
@NoArgsConstructor
public class ServiceDetailResponseDto {
    private Long id;
    
	private ServiceType serviceType;
    
	private ProviderSummaryResponseDto provider;
    
	private String name;
    
	private String description;
    
    private List<ServiceLocaleResponseDto> serviceLocale;
	
	private BigDecimal basePrice;
    
	private List<String> images;
    
    private Boolean publicationStatus;
    
    private boolean available;
}
