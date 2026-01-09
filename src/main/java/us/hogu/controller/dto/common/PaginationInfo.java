package us.hogu.controller.dto.common;

import lombok.Data;

@Data
public class PaginationInfo {
    private int currentPage;
    private int pageSize;
    private int totalPages;
    private long totalElements;
    private boolean hasNext;
    private boolean hasPrevious;
}
