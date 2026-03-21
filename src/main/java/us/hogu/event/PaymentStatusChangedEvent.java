package us.hogu.event;

import org.springframework.context.ApplicationEvent;
import us.hogu.model.Payment;

public class PaymentStatusChangedEvent extends ApplicationEvent {
    private final Payment payment;

    public PaymentStatusChangedEvent(Object source, Payment payment) {
        super(source);
        this.payment = payment;
    }

    public Payment getPayment() {
        return payment;
    }
}
