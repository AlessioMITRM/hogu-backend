package us.hogu.common.constants;

import org.springframework.context.i18n.LocaleContextHolder;
import java.util.Locale;

public enum ErrorConstants {

    GENERIC_ERROR(
        "Si è verificato un errore inatteso. Riprova più tardi.",
        "An unexpected error occurred. Please try again later."
    ),
    ACCESS_DENIED(
        "Non hai i privilegi necessari per accedere a questa risorsa.",
        "You do not have permission to access this resource."
    ),
    UNAUTHORIZED(
        "Non sei autenticato. Effettua il login per accedere a questa risorsa.",
        "You are not authenticated. Please log in to access this resource."
    ),
    GENERIC_ERROR_GET_STORE_PROCEDURE(
        "Si è verificato un errore nel recupero della store procedure.",
        "An error occurred while retrieving the stored procedure."
    ),
    USER_EMAIL_ALREADY_EXISTS(
        "Email già registrata.",
        "Email already registered."
    ),
    OTP_ALREADY_EXISTS(
            "E' gia stata richieata una verifica dell'otp.",
            ""
    ),
    USER_CREDENTIAL_NOT_VALID(
        "Credenziali non valide.",
        "Invalid credentials."
    ),
    USER_NOT_FOUND(
        "Utente non trovato.",
        "User not found."
    ),
    USER_NOT_AUTHORIZED(
            "Utente non autorizzato.",
            "User not found."
    ),
    EVENT_NOT_FOUND(
        "Evento non trovato.",
        "Event not found."
    ),
    INVALID_APPOINTMENT_DATE(
        "Data dell'appuntamento non valida.",
        "Invalid appointment date."
    ),
    USER_ROLE_INVALID(
        "Tipo ruolo utente non valido per effettuare l'operazione.",
        "Invalid user role for this operation."
    ),
    VALIDATION_ERROR(
        "Validazione dei parametri nella richiesta fallita.",
        "Parameter validation failed."
    ),
    RESOURCE_NOT_FOUND(
        "Risorsa non trovata.",
        "Resource not found."
    ),
    PARAMS_NOT_VALID(
        "I parametri inviati nella richiesta non sono validi.",
        "The parameters sent in the request are invalid."
    ),
    GENERIC_ERROR_AUTHORIZATION(
        "Si è verificato un errore inatteso. Riprova più tardi.",
        "An unexpected error occurred. Please try again later."
    ),
    FILE_MAX_SIZE(
        "Il seguente file supera 1 MB: ",
        "This file exceeds 1 MB: "
    ),
    LIMIT_MAX_PEOPLE_RESTURANT(
        "Numero di persone superiore alla capacità del ristorante.",
        "Number of people exceeds restaurant capacity."
    ),
    NOT_AVAILABILITY_NCC_FOR_TIME(
        "Servizio NCC non disponibile per l'orario selezionato.",
        "NCC service not available for the selected time."
    ),
    LIMIT_MAX_PEOPLE_CLUB(
        "Numero di persone superiore alla capacità del club.",
        "Number of people exceeds club capacity."
    ),
    INSUFFICIENT_AVAILABLE_SEATS(
        "Non ci sono abbastanza posti disponibili per l'orario selezionato.",
        "Not enough available seats for the selected time."
    ),
    EXCEEDED_LUGGAGE_CAPACITY(
        "Numero di bagagli superiore alla capacità del deposito.",
        "Number of luggage items exceeds storage capacity."
    ),
    UNAUTHORIZED_LUGGAGE_SERVICE(
        "Servizio bagagli non autorizzato.",
        "Unauthorized luggage service."
    ),
    RESTURANT_NOT_FOUND(
            "Ristorante non trovato.",
            ""
    ),
    PROVIDER_NOT_FOUND(
            "Fornitore non trovato.",
            ""
    ),
    SERVICE_NOT_FOUND(
            "Servizio non trovato.",
            ""
    ),
    RESTURANT_NOT_FOUND_OR_NOT_AUTHORIZED(
            "Ristorante non trovato o non autorizzato.",
            ""
    ),
    SERVICE_NCC_NOT_FOUND(
            "Servizio NCC non trovato",
            ""
    ),
    SERVICE_NCC_NOT_FOUND_OR_NOT_AUTHORIZED(
            "Servizio NCC non trovato o non autorizzato.",
            ""
    ),
    CLUB_NOT_FOUND(
            "Club non trovato",
            ""
    ), 
    CLUB_NOT_FOUND_OR_NOT_AUTHORIZED(
            "Club non trovato o non autorizzato",
            ""
    ), 
    SERVICE_LUGGAGE_NOT_FOUND(
            "Servizio bagagli non trovato",
            ""
    ), 
    ERROR_PAYMENT_STRIPE(
    		"Si è verificato un errore durante il pagamento con stripe",
    		""),
    PAYMENT_NOT_FOUND(
    		"Si è verificato un errore nella ricerca del pagamento",
    		""),
    ERROR_REFUND_STRIPE(
    		"Si è verificato un Errore durante il rimborso Stripe",
    		""),
    ERROR_PAYMENT_DELETE(
    		"Si è verificato un errore durante la cancellazione pagamento",
    		""),
    ERROR_PAYMENT_PAYPAL(
    		"Errore durante la creazione pagamento PayPal",
    		""),
    ERROR_REFUND_PAYPAL(
    		"Si è verificato un Errore durante il rimborso PayPal",
    		""),
    ERROR_PAYMENT_DELETE_PAYPAL(
    		"Errore durante la cancellazione pagamento PayPal",
    		""),
    UNAUTHORIZED_BOOKING(
            "lA Prenotazione non appartiene all'utente",
            ""
        ),
    UNAUTHORIZED_PAYMENT(
            "Pagamento non autorizzato ",
            "."
        ),
    SERVICE_TYPE_NOT_VALID(
            "Tipo del servizio non valido",
            ""
        ),
    BOOKING_NCC_NOT_FOUND(
            "Servizio NCC non trovato",
            ""
    ),
    BOOKING_CLUB_NOT_FOUND(
            "Servizio Club non trovato",
            ""
    ),
    BOOKING_RESTURANT_NOT_FOUND(
            "Servizio Ristorante non trovato",
            ""
    ),
    BOOKING_LUGGAGE_NOT_FOUND(
            "Servizio Bagagli non trovato",
            ""
    ),
    REFUND_NOT_ALLOWED(
            "Rimborso non consentito",
            ""
    ),
    OTP_NOT_FOUND(
            "Nessun Otp trovato associato a questo account ancora in corso di validità",
            ""
    ),
    OTP_NOT_VALID(
            "L'otp non inserito non è corretto",
            ""
    ),
    USER_SERVICE_NOT_FOUND(
            "Nessun Documento associato all'account fornitore è stato trovato",
            ""
    ),
    RESTAURANT_NOT_FOUND(
            "Nessun ristorante trovato",
            ""
    ),
    SERVICE_BNB_NOT_FOUND(
            "Nessun bnb trovato",
            ""
    ),
    INVALID_AMOUNT(
            "L'importo non è valido",
            ""
    ),
    BOOKING_RESTAURANT_NOT_FOUND(
            "Nessuna prenotazione del ristorante è stata trovata",
            ""
    ),
    DOCUMENT_NOT_FOUND(
            "Nessun Documento Trovato",
            ""
    ),
    ACCOUNT_NOT_ACTIVE(
            "L'account non è attivo, impossibile proseguire con l'operazione",
            ""
    ),
    OTP_ALREADY_VALID(
            "L'otp già è stato validato",
            ""
    ),
    PROVIDER_NOT_ALLOWED(
            "Il fornitore non è abilitato per questa operazione",
            ""
    ),
    FORMAT_IMAGE_NOT_ALLOWED(
            "Il formato dell'immagine non è consentito",
            ""
    ),
    IMAGES_REQUIRED(
            "L'immagine è obbligatoria",
            ""
    ),
    ;
	

    private final String messageIt;
    private final String messageEn;

    ErrorConstants(String messageIt, String messageEn) {
        this.messageIt = messageIt;
        this.messageEn = messageEn;
    }

    /**
     * Restituisce il messaggio nella lingua corretta in base al locale dell'utente.
     * Se la lingua non è riconosciuta, viene usato l'italiano come default.
     */
    public String getMessage() {
        Locale locale = LocaleContextHolder.getLocale();
        if (locale != null && locale.getLanguage().equalsIgnoreCase("en")) {
            return messageEn;
        }
        return messageIt;
    }
}
