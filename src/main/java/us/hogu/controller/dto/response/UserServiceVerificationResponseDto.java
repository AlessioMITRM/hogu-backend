package us.hogu.controller.dto.response;

import java.time.OffsetDateTime;
import java.util.List;

import lombok.Data;
import us.hogu.model.enums.ServiceType;
import us.hogu.model.enums.VerificationStatusServiceEY;

@Data
public class UserServiceVerificationResponseDto {
    private Long id;
    
    private ServiceType serviceType;
    
    private boolean licenseValid;
    
    private boolean vatValid;
    
    private VerificationStatusServiceEY verificationStatus;
    
    private OffsetDateTime lastUpdateDate;

    private List<UserDocumentResponseDto> documents;
}
