package us.hogu.model.enums;

public enum UserStatus {
    PENDING("IN_ATTESA", 0),        // registrazione in attesa OTP
    ACTIVE("ATTIVO", 1),            // account attivo
    SUSPENDED("SOSPESO", 2),        // sospeso temporaneamente
    DEACTIVATED("DISATTIVATO", 3),  // account disattivato
    PENDING_ADMIN_APPROVAL("IN_ATTESA_APPROVAZIONE_ADMIN", 4); // account in attesa di attivazione dell'admin

    private final String italianValue;
    private final int statusId;

    UserStatus(String italianValue, int statusId) {
        this.italianValue = italianValue;
        this.statusId = statusId;
    }

    public String getItalianValue() {
        return italianValue;
    }

    public int getStatusId() {
        return statusId;
    }

    public static UserStatus fromItalianValue(String italianValue) {
        for (UserStatus status : values()) {
            if (status.italianValue.equals(italianValue)) {
                return status;
            }
        }
        throw new IllegalArgumentException("Valore italiano non valido: " + italianValue);
    }

    public static UserStatus fromName(String name) {
        for (UserStatus status : values()) {
            if (status.name().equalsIgnoreCase(name)) {
                return status;
            }
        }
        throw new IllegalArgumentException("Valore non valido: " + name);
    }
}
