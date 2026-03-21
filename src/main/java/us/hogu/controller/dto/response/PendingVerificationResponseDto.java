package us.hogu.controller.dto.response;

import java.time.OffsetDateTime;
import java.util.List;

import lombok.Data;
import us.hogu.model.enums.ServiceType;
import us.hogu.model.enums.VerificationStatusServiceEY;

@Data
public class PendingVerificationResponseDto {
    private Long verificationId;

    private Long userId;

    private String providerName;

    private String email;

    private ServiceType serviceType;

    private OffsetDateTime requestDate;

    private VerificationStatusServiceEY status;

    private List<UserDocumentResponseDto> documents;

    private boolean licenseValid;

    private boolean vatValid;

    private String description;

    private String iban;
}
