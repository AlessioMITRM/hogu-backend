package us.hogu.controller.dto.response;

import lombok.Data;

@Data
public class UserDocumentResponseDto {
	
    private Long id;
    
    private String filename;
    
    byte[] fileData;
    
    private boolean approved;
}
