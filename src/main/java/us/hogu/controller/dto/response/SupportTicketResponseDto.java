package us.hogu.controller.dto.response;

import java.util.List;

import us.hogu.controller.dto.common.PaginationInfo;
import us.hogu.controller.dto.common.SupportTicketDto;
import lombok.Data;

@Data
public class SupportTicketResponseDto {
    private List<SupportTicketDto> tickets;
    
    private PaginationInfo pagination;
}
