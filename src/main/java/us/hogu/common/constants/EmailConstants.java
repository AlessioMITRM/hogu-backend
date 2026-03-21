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
                                        "Grazie,\nIl team Hogu.",
                        "Your Hogu verification code",
                        "Hello,\n\nyour verification code is: %s\n\n" +
                                        "Enter it in the app to complete the security procedure.\n\n" +
                                        "This code will expire in a few minutes for security reasons.\n\n" +
                                        "Thank you,\nThe Hogu team."),

        // ❌ Rifiuto dell'account
        ACCOUNT_REJECTION(
                        "Richiesta di registrazione rifiutata",
                        "Ciao,\n\npurtroppo la tua richiesta di registrazione è stata rifiutata.\n\n" +
                                        "Motivazioni:\n%s\n\n" +
                                        "Se ritieni che si tratti di un errore o vuoi riprovare, " +
                                        "contatta il nostro team di supporto o invia una nuova richiesta.\n\n" +
                                        "Grazie per la comprensione,\nIl team Hogu.",
                        "Registration request rejected",
                        "Hello,\n\nunfortunately your registration request has been rejected.\n\n" +
                                        "Reasons:\n%s\n\n" +
                                        "If you believe this is an error or want to try again, " +
                                        "contact our support team or submit a new request.\n\n" +
                                        "Thank you for your understanding,\nThe Hogu team."),

        // ✉️ Conferma account
        ACCOUNT_VERIFICATION(
                        "Conferma il tuo account",
                        "Ciao,\n\nper completare la registrazione, clicca sul link di verifica qui sotto:\n\n%s\n\n" +
                                        "Grazie,\nIl team Hogu.",
                        "Confirm your account",
                        "Hello,\n\nto complete the registration, click on the verification link below:\n\n%s\n\n" +
                                        "Thank you,\nThe Hogu team."),

        // 🔑 Reset password
        PASSWORD_RESET(
                        "Reimposta la tua password",
                        "Ciao,\n\nhai richiesto di reimpostare la tua password. Questo è il tuo otp:\n\n%s\n\n",
                        "Reset your password",
                        "Hello,\n\nyou requested to reset your password. This is your OTP:\n\n%s\n\n"),

        // 📅 Conferma prenotazione
        BOOKING_CONFIRMATION(
                        "Conferma prenotazione",
                        "Ciao,\n\nla tua prenotazione è stata confermata! Dettagli:\n\n%s\n\n" +
                                        "Grazie per aver scelto Hogu.",
                        "Booking confirmation",
                        "Hello,\n\nyour booking has been confirmed! Details:\n\n%s\n\n" +
                                        "Thank you for choosing Hogu."),

        // ❌ Cancellazione prenotazione
        BOOKING_CANCELLATION(
                        "Cancellazione prenotazione",
                        "Ciao,\n\nla tua prenotazione è stata cancellata.\n\n%s\n\n" +
                                        "Per maggiori informazioni, contatta il supporto.",
                        "Booking cancellation",
                        "Hello,\n\nyour booking has been cancelled.\n\n%s\n\n" +
                                        "For more information, contact support."),

        // ✅ Attivazione account
        ACCOUNT_ACTIVATION(
                        "Il tuo account Hogu è attivo!",
                        "Ciao,\n\nsiamo lieti di comunicarti che il tuo account Hogu è stato attivato.\n" +
                                        "Ora puoi accedere a tutti i servizi della piattaforma.\n\n" +
                                        "Grazie per essere parte di Hogu!",
                        "Your Hogu account is active!",
                        "Hello,\n\nwe are pleased to inform you that your Hogu account has been activated.\n" +
                                        "You can now access all the services of the platform.\n\n" +
                                        "Thank you for being part of Hogu!"),

        // ⛔ Sospensione/Disattivazione account
        ACCOUNT_DEACTIVATION(
                        "Il tuo account Hogu è stato disattivato",
                        "Ciao,\n\nti informiamo che il tuo account Hogu è stato temporaneamente disattivato o sospeso.\n"
                                        + "Se ritieni che si tratti di un errore, contatta il nostro supporto.\n\n" +
                                        "Il team Hogu.",
                        "Your Hogu account has been deactivated",
                        "Hello,\n\nwe inform you that your Hogu account has been temporarily deactivated or suspended.\n"
                                        + "If you believe this is an error, please contact our support.\n\n" +
                                        "The Hogu team."),

        // 🚫 Ban account
        ACCOUNT_BANNED(
                        "Account bannato permanentemente",
                        "Ciao,\n\nti informiamo che il tuo account Hogu è stato bannato permanentemente a causa di violazioni dei nostri termini di servizio.\n"
                                        + "L'accesso alla piattaforma non è più consentito.\n\n" +
                                        "Il team Hogu.",
                        "Account permanently banned",
                        "Hello,\n\nwe inform you that your Hogu account has been permanently banned due to violations of our terms of service.\n"
                                        + "Access to the platform is no longer allowed.\n\n" +
                                        "The Hogu team."),

        // 🗑️ Cancellazione account
        ACCOUNT_DELETION(
                        "Il tuo account Hogu è stato cancellato",
                        "Ciao,\n\nti confermiamo che la tua richiesta di cancellazione dell'account Hogu è stata elaborata con successo.\n"
                                        + "Tutti i tuoi dati personali sono stati anonimizzati in conformità con le nostre policy sulla privacy.\n\n"
                                        + "Ci dispiace vederti andare via, speriamo di rivederti in futuro!\n\n" +
                                        "Il team Hogu.",
                        "Your Hogu account has been deleted",
                        "Hello,\n\nwe confirm that your request to delete your Hogu account has been successfully processed.\n"
                                        + "All your personal data has been anonymized in accordance with our privacy policies.\n\n"
                                        + "We are sorry to see you go, we hope to see you again in the future!\n\n" +
                                        "The Hogu team.");

        private final String subjectIt;
        private final String bodyIt;
        private final String subjectEn;
        private final String bodyEn;

        public String getSubject(String lang) {
                if ("en".equalsIgnoreCase(lang)) {
                        return subjectEn;
                }
                return subjectIt;
        }

        public String getTextBody(String lang) {
                if ("en".equalsIgnoreCase(lang)) {
                        return bodyEn;
                }
                return bodyIt;
        }

}
