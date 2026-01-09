package us.hogu.controller.dto.request;

import java.util.List;

import lombok.Data;

@Data
public class MarkAsReadRequestDto {
    private List<Long> notificationIds;
}
