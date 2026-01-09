package us.hogu.controller.dto.response;

import java.util.Date;

import lombok.Data;

@Data
public class OperationResponseDto {
    private String code;
    private String message;
    private Date timestamp;   
    
    public OperationResponseDto(String code, String message, Date timestamp) {
        this.code = code;
        this.message = message;
        this.timestamp = timestamp;
    }
}
