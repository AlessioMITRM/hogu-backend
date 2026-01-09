package us.hogu.controller.dto.request;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import org.springframework.web.multipart.MultipartFile;
import lombok.Data;

@Data
public class UserDocumentRequestDto {

    private String filename;

    private MultipartFile file;

    private boolean approved;
}

