package us.hogu.controller.dto.response;

import java.util.List;
import lombok.Data;

@Data
public class PendingUserResponseDto {
	
    private long idUser;
    
    private String name;
    
    private List<UserServiceVerificationResponseDto> userServiceverifications;
}
