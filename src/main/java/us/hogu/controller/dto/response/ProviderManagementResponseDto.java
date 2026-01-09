package us.hogu.controller.dto.response;

import java.util.List;

import us.hogu.controller.dto.common.PaginationInfo;
import us.hogu.controller.dto.common.ProviderInfoDto;
import lombok.Data;

@Data
public class ProviderManagementResponseDto {
    private List<ProviderInfoDto> providers;
    
    private PaginationInfo pagination;
}
