package us.hogu.model.enums;

public enum BookingStatus {
    PENDING("IN_ATTESA"),
    DEPOSIT_PAID("ANTICIPO_PAGATO"),
    COMPLETED("COMPLETATO"),
    CANCELLED_BY_PROVIDER("ANNULLATA_FORNITORE"),
    MODIFIED_BY_PROVIDER("MODIFICA_FORNITORE"),
    REFUNDED_BY_ADMIN("RIMBORSATO_ADMIN"),
    CANCELLED_BY_ADMIN("ANNULLATA_ADMIN"),
    FULL_PAYMENT_COMPLETED("PAGAMENTO_COMPLETO_ESEGUITO"),
    WAITING_PROVIDER_CONFIRMATION("IN_ATTESA_CONFERMA_DEL_FORNITORE"),
    WAITING_CUSTOMER_PAYMENT("IN_ATTESA_PAGAMENTO_DEL_CLIENTE");
    
    private final String italianValue;
    
    BookingStatus(String italianValue) {
        this.italianValue = italianValue;
    }
    
    public String getItalianValue() {
        return italianValue;
    }
    
    public static BookingStatus fromItalianValue(String italianValue) {
        for (BookingStatus status : values()) {
            if (status.italianValue.equals(italianValue)) {
                return status;
            }
        }
        throw new IllegalArgumentException("Valore italiano non valido: " + italianValue);
    }
}
