package us.hogu.model.enums;

/**
 * Enum che gestisce gli stati del ciclo di vita di una prenotazione.
 * 
 * <p>
 * <strong>LEGENDA SERVIZI:</strong>
 * </p>
 * <ul>
 * <li><strong>[ALL]</strong>: Tutti i servizi (Ristoranti, NCC, Club, B&B,
 * Luggage)</li>
 * <li><strong>[PREPAID]</strong>: Servizi con pagamento anticipato online (NCC,
 * Club, B&B, Luggage)</li>
 * <li><strong>[POSTPAID]</strong>: Servizi con pagamento in loco
 * (Ristoranti)</li>
 * </ul>
 */
public enum BookingStatus {

    /**
     * [PREPAID] Stato iniziale tecnico. La prenotazione è stata creata ma il
     * processo di pagamento non è iniziato.
     */
    PENDING("IN_ATTESA"),

    /**
     * [PREPAID] Il cliente ha autorizzato il pagamento (Pre-Authorization).
     * I fondi sono bloccati ma non ancora prelevati. In attesa di accettazione del
     * fornitore.
     */
    PAYMENT_AUTHORIZED("PAGAMENTO_AUTORIZZATO"),

    /**
     * [ALL] Il servizio è stato erogato e concluso con successo.
     * - Per [PREPAID]: I fondi sono già stati incassati da Hogu.
     * - Per [POSTPAID]: Il cliente ha onorato la prenotazione.
     */
    COMPLETED("COMPLETATO"),

    /**
     * [ALL] La prenotazione è stata annullata dal fornitore.
     * - Se c'era un pagamento (Auth/Capture), viene rimborsato/stornato.
     */
    CANCELLED_BY_PROVIDER("ANNULLATA_FORNITORE"),

    /**
     * [ALL] La prenotazione è stata modificata dal fornitore (es. cambio
     * orario/prezzo).
     * Richiede nuova conferma utente.
     */
    MODIFIED_BY_PROVIDER("MODIFICA_FORNITORE"),

    /**
     * [PREPAID] Rimborso manuale effettuato dagli amministratori di Hogu.
     */
    REFUNDED_BY_ADMIN("RIMBORSATO_ADMIN"),

    /**
     * [ALL] Cancellazione forzata dagli amministratori di Hogu (es. frode, errore
     * tecnico).
     */
    CANCELLED_BY_ADMIN("ANNULLATA_ADMIN"),

    /**
     * [PREPAID] Il fornitore ha accettato la prenotazione e Hogu ha incassato i
     * soldi (Capture).
     * La prenotazione è confermata e pagata.
     */
    FULL_PAYMENT_COMPLETED("PAGAMENTO_COMPLETO_ESEGUITO"),

    /**
     * [POSTPAID] (Principalmente Ristoranti). Il cliente ha inviato la richiesta.
     * Il fornitore deve ancora accettare o rifiutare. Nessun pagamento coinvolto.
     */
    WAITING_PROVIDER_CONFIRMATION("IN_ATTESA_CONFERMA_DEL_FORNITORE"),

    /**
     * [PREPAID] La richiesta è stata accettata (o è istantanea), ma il cliente deve
     * completare il checkout.
     * (es. 3D Secure fallito o abbandono carrello).
     */
    WAITING_CUSTOMER_PAYMENT("IN_ATTESA_PAGAMENTO_DEL_CLIENTE"),

    /**
     * [PREPAID] (NCC, Club, B&B). Stato FINALE Amministrativo.
     * Indica che Hogu ha effettuato il bonifico al fornitore per questa
     * prenotazione
     * (Netto = Totale - Commissioni Hogu).
     */
    PROVIDER_LIQUIDATED("LIQUIDATO_AL_FORNITORE"),

    /**
     * [POSTPAID] (Ristoranti). Stato FINALE Amministrativo.
     * Indica che il Ristorante ha pagato a Hogu le commissioni dovute per questa
     * prenotazione
     * (es. tramite fattura mensile riepilogativa).
     */
    COMMISSION_PAID("COMMISSIONI_PAGATE"),

    WAITING_COMPLETION("IN_ATTESA_DI_COMPLETAMENTO"),

    /**
     * [ALL] La prenotazione è stata completata manualmente dall'amministratore.
     */
    COMPLETED_BY_ADMIN("COMPLETATA_ADMIN");

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
