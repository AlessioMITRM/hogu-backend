package us.hogu.controller.dto.response;

import java.util.List;

import lombok.Data;

@Data
public class NotificationListResponseDto {
    private List<NotificationResponseDto> notifications;
    
    private Integer unreadCount;
}
