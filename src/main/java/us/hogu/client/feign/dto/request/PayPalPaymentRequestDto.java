package us.hogu.client.feign.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import us.hogu.model.enums.ServiceType;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PayPalPaymentRequestDto {
    private Double amount;
    private String currency;
    private Long bookingId;
    private Long userId;
    private ServiceType serviceType;
    private String description;
    private String customerEmail;
    private List<PayPalItem> items;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PayPalItem {
        private String name;
        private String description;
        private Double price;
        private Integer quantity;
        private String sku;
    }
}
