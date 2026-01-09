package us.hogu.common.constants;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum EmailConstants {

    // 🔐 OTP di verifica
    OTP_VERIFICATION(
        "Il tuo codice di verifica Hogu",
        "Ciao,\n\nil tuo codice di verifica è: %s\n\n" +
        "Inseriscilo nell'app per completare la procedura di sicurezza.\n\n" +
        "Questo codice scadrà tra pochi minuti per motivi di sicurezza.\n\n" +
        "Grazie,\nIl team Hogu."
    ),

    // ❌ Rifiuto dell'account
    ACCOUNT_REJECTION(
        "Richiesta di registrazione rifiutata",
        "Ciao,\n\npurtroppo la tua richiesta di registrazione è stata rifiutata.\n\n" +
        "Motivazioni:\n%s\n\n" +
        "Se ritieni che si tratti di un errore o vuoi riprovare, " +
        "contatta il nostro team di supporto o invia una nuova richiesta.\n\n" +
        "Grazie per la comprensione,\nIl team Hogu."
    ),

    // ✉️ Conferma account
    ACCOUNT_VERIFICATION(
        "Conferma il tuo account",
        "Ciao,\n\nper completare la registrazione, clicca sul link di verifica qui sotto:\n\n%s\n\n" +
        "Grazie,\nIl team Hogu."
    ),

    // 🔑 Reset password
    PASSWORD_RESET(
        "Reimposta la tua password",
        "Ciao,\n\nhai richiesto di reimpostare la tua password. Questo è il tuo otp:\n\n%s\n\n"
    ),

    // 📅 Conferma prenotazione
    BOOKING_CONFIRMATION(
        "Conferma prenotazione",
        "Ciao,\n\nla tua prenotazione è stata confermata! Dettagli:\n\n%s\n\n" +
        "Grazie per aver scelto Hogu."
    ),

    // ❌ Cancellazione prenotazione
    BOOKING_CANCELLATION(
        "Cancellazione prenotazione",
        "Ciao,\n\nla tua prenotazione è stata cancellata.\n\n%s\n\n" +
        "Per maggiori informazioni, contatta il supporto."
    );

    private final String object;
    private final String textBody;
}
