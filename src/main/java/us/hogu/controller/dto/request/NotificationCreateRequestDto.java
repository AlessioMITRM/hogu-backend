package us.hogu.controller.dto.request;

import lombok.Data;

@Data
public class NotificationCreateRequestDto {
	private Long userId;
	
    private String title;
    
    private String message;
}
