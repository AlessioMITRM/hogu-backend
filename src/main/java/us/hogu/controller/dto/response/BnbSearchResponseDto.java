package us.hogu.controller.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
public class BnbSearchResponseDto {
    private List<BnbSearchResultDto> content;
    private int totalPages;
    private long totalElements;
    private int currentPage;
    private int pageSize;

    @Data
    @Builder
    public static class BnbSearchResultDto {
        private String id;
        private String name;
        private List<ServiceLocaleResponseDto> locales;
        private String description;
        private BigDecimal price;
        private BigDecimal pricePerNight;
        private Integer maxGuests;
        private Integer roomCount;
        private List<String> amenities;
        private List<String> images;
        private Coordinates coordinates;

        @Data
        @Builder
        public static class Coordinates {
            private Double latitude;
            private Double longitude;
        }
    }
}
