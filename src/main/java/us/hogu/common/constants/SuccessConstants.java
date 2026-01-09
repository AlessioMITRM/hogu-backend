package us.hogu.common.constants;

public enum SuccessConstants {
	SUCCESS(""),
	GENERAL_SUCCESS("Operazione eseguita con successo"),
	APPOINTMENT_DELETE("Si è verificato un errore inatteso. Riprova più tardi."),
	ACCESS_DENIED("Non hai i privilegi necessari per accedere a questa risorsa."),
	ASSIGNMENT_SUCCESS("Presa in carico effettuata con successo"),
	HANDLING_CANCELLATION_SUCCESS("Presa in carico annullata con successo"),
	UPDATE_SUCCESS("Aggiornamento effettuato con successo"),
	APPOINTMENT_INSERT_SUCCESSFULLY("Appuntamento inserito correttamente"),
	APPOINTMENT_UPDATED_SUCCESSFULLY("Appuntamento aggiornato correttamente"),
	SUPPORT_REQUEST_CREATED_SUCCESSFULLY("Richiesta di supporto inserita correttamente"),
	SUPPORT_REQUEST_UPDATED_SUCCESSFULLY("Richiesta di supporto aggiornata correttamente"),
	SUPPORT_REQUEST_DELETED_SUCCESSFULLY("Richiesta di supporto eliminata correttamente"),
	FEEDBACK_CREATED_SUCCESSFULLY("Feedback inserito correttamente"),
	USER_FARMER_ALREADY_EXISTS_MESSAGE("Utente e cuaa già presenti in banca dati"),
	USER_FARMER_REGISTRATION_SUCCESS_MESSAGE("Registrazione avvenuta con successo"),
	USER_OPERATOR_IS_REGISTERED("L'operatore è censito in banca dati");
	;

    private final String message;

    SuccessConstants(String message) {
        this.message = message;
    }

    public String getMessage() {
        return message;
    }
}
