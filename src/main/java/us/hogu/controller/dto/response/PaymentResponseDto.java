package us.hogu.controller.dto.response;

import java.time.OffsetDateTime;

import us.hogu.model.enums.PaymentMethod;
import us.hogu.model.enums.PaymentStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PaymentResponseDto {
	private Long id;
	
	private Long bookingId;
	
	private Long userId;
	
	private Double amount;
	
	private String currency;
	
	private PaymentMethod paymentMethod;
	
	private PaymentStatus status;
	
	private Double feeAmount;
	
	private Double netAmount;
	
	private OffsetDateTime createdAt;
}
