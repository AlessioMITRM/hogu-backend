package us.hogu.model.enums;

public enum VerificationStatusServiceEY {
    PENDING("IN_ATTESA", 0),        // ATTESA APPROVAZIONE ADMIN
    ACTIVE("ATTIVO", 1),            // SERVIZIO ATTIVO
    REFUSED("RIFIUTATO", 3),        // SERVIZIO RIFIUTATO
    SUSPENDED("SOSPESO", 2);        // SERVIZIO SOSPESO

    private final String italianValue;
    private final int statusId;

    VerificationStatusServiceEY(String italianValue, int statusId) {
        this.italianValue = italianValue;
        this.statusId = statusId;
    }

    public String getItalianValue() {
        return italianValue;
    }

    public int getStatusId() {
        return statusId;
    }

    public static VerificationStatusServiceEY fromItalianValue(String italianValue) {
        for (VerificationStatusServiceEY status : values()) {
            if (status.italianValue.equals(italianValue)) {
                return status;
            }
        }
        throw new IllegalArgumentException("Valore italiano non valido: " + italianValue);
    }

    public static VerificationStatusServiceEY fromName(String name) {
        for (VerificationStatusServiceEY status : values()) {
            if (status.name().equalsIgnoreCase(name)) {
                return status;
            }
        }
        throw new IllegalArgumentException("Valore non valido: " + name);
    }
}
