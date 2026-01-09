package us.hogu.model.enums;

public enum SupportTicketStatus {
    OPEN("APERTO"),                           // Ticket appena creato
    IN_PROGRESS("IN_LAVORAZIONE"),            // Preso in carico dal supporto
    AWAITING_USER_RESPONSE("IN_ATTESA_RISPOSTA"), // In attesa di risposta dall'utente
    RESOLVED("RISOLTO"),                      // Problema risolto
    CLOSED("CHIUSO"),                         // Ticket chiuso
    CANCELLED("ANNULLATO");                   // Ticket annullato

    private final String value;

    SupportTicketStatus(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    // Metodo per ottenere l'enum dal valore italiano
    public static SupportTicketStatus fromValue(String value) {
        for (SupportTicketStatus status : SupportTicketStatus.values()) {
            if (status.value.equals(value)) {
                return status;
            }
        }
        throw new IllegalArgumentException("Stato ticket non valido: " + value);
    }
}
