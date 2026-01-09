package us.hogu.model.enums;

public enum PaymentStatus {
    PENDING("IN_ATTESA"),
    COMPLETED("COMPLETATO"),
    FAILED("FALLITO");
    
    private final String italianValue;
    
    PaymentStatus(String italianValue) {
        this.italianValue = italianValue;
    }
    
    public String getItalianValue() {
        return italianValue;
    }
    
    public static PaymentStatus fromItalianValue(String italianValue) {
        for (PaymentStatus status : values()) {
            if (status.italianValue.equals(italianValue)) {
                return status;
            }
        }
        throw new IllegalArgumentException("Valore italiano non valido: " + italianValue);
    }
}
