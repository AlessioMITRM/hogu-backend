package us.hogu.common.constants;

public final class SwaggerConstants {
    private SwaggerConstants() {}
    // PRINCIPAL COSTANTS
    public static final String QUERY = "Query";
    public static final String COMMAND = "Command";
    public static final String SEPARATOR = " - ";
    
    // TAG NAME
    public static final String TAG_NAME_OPERATOR = "Operatore";
    public static final String TAG_NAME_FARMER = "Agricoltore";
    public static final String TAG_NAME_SUPPORT_REQUESTS = "Richieste di supporto";
    public static final String TAG_NAME_USER_FARMER = "Utente Agricoltore";
    public static final String TAG_NAME_USER_OPERATOR = "Utente Operatore";
    public static final String TAG_NAME_NOTIFICATIONS = "Notifiche";
    public static final String TAG_NAME_NOTIFICATION = "Notifica";
    
    // TAG NAME COMAND COMPLETE
    public static final String TAG_NAME_COMPLETE_OPERATOR_COMMAND = TAG_NAME_OPERATOR + SEPARATOR + COMMAND;
    public static final String TAG_NAME_COMPLETE_FARMER_COMMAND = TAG_NAME_FARMER + SEPARATOR + COMMAND;
    public static final String TAG_NAME_COMPLETE_SUPPORT_REQUESTS_COMAND = TAG_NAME_SUPPORT_REQUESTS + SEPARATOR + COMMAND;
    public static final String TAG_NAME_COMPLETE_USER_FARMER_COMMAND = TAG_NAME_USER_FARMER + SEPARATOR + COMMAND;
    public static final String TAG_NAME_COMPLETE_USER_OPERATOR_COMMAND = TAG_NAME_USER_OPERATOR + SEPARATOR + COMMAND;
    public static final String TAG_NAME_COMPLETE_NOTIFICATION_COMMAND = TAG_NAME_NOTIFICATIONS + SEPARATOR + COMMAND;

    // TAG NAME QUERY COMPLETE
    public static final String TAG_NAME_COMPLETE_OPERATOR_QUERY = TAG_NAME_OPERATOR + SEPARATOR + QUERY;
    public static final String TAG_NAME_COMPLETE_FARMER_QUERY = TAG_NAME_FARMER + SEPARATOR + QUERY;
    public static final String TAG_NAME_COMPLETE_SUPPORT_REQUESTS_QUERY = TAG_NAME_SUPPORT_REQUESTS + SEPARATOR + QUERY;
    public static final String TAG_NAME_COMPLETE_NOTIFICATION_QUERY = TAG_NAME_NOTIFICATIONS + SEPARATOR + QUERY;
    
    // TAG DESCRIPTION QUERY
    public static final String TAG_DESCRIPTION_QUERY_OPERATOR = "API per ottenere dati sugli appuntamenti";
    public static final String TAG_DESCRIPTION_QUERY_FARMER = "API per ottenere dati sugli appuntamenti";
    public static final String TAG_DESCRIPTION_QUERY_SUPPORT_REQUESTS = "API per ottenere dati sulle richieste di supporto";
    public static final String TAG_DESCRIPTION_QUERY_NOTIFICATION = "API per ottenere dati sulle notifiche";

    // TAG DESCRIPTION COMMAND
    public static final String TAG_DESCRIPTION_COMMAND_OPERATOR = "API per creare, aggiornare o cancellare operatori";
    public static final String TAG_DESCRIPTION_COMMAND_FARMER = "API per creare, aggiornare o cancellare agricoltori";
    public static final String TAG_DESCRIPTION_COMMAND_SUPPORT_REQUESTS = "API per creare, aggiornare o cancellare richieste di supporto";
    public static final String TAG_DESCRIPTION_COMMAND_USER = "API per creare, aggiornare o cancellare Utenti";
    public static final String TAG_DESCRIPTION_COMMAND_NOTIFICATION = "API per creare, aggiornare o cancellare notifiche";

    // EXTRA INFO
    public static final String INFO_FOR_BOOKING_CONFIRMATION =
    	    "Questa API richiede test tramite multipart/form-data con FormData; lo Swagger UI non funzionerà per questo tipo di richiesta.\n" +
    	    "Esempio di test tramite curl per l'invio di due file:\n\n" +
    	    "curl -X POST \"http://localhost:8080/api/v1/agricoltore/confermaPrenotazione\" -H \"X-CODE-CUAA: cua_01_prova\" -H \"Authorization: Bearer eyJhbGciOiJSUzI1NiIsInR5cCIgOiAiSldUIiwia2lkIiA6ICI1ZDA5WDRkenBRUGJNbnFCZWNDZnVDZ3pwSFd0eWduTnI0VzE4d0lrM0tJIn0.eyJleHAiOjE3NjAwMDIyNDAsImlhdCI6MTc2MDAwMTk0MCwianRpIjoiMGM2ZjFkMzUtZDFjZS00MGY2LThlZjItMDBkZjEyN2I3NzRhIiwiaXNzIjoiaHR0cHM6Ly9zc28tcHJlLmFnZWEuZ292Lml0L2F1dGgvcmVhbG1zL2FnZWFfc3NvX3JlYWxtIiwiYXVkIjoiYWNjb3VudCIsInN1YiI6IjFmMDE1ZGE2LWZkMjEtNDY4Yi1hZDEwLWFlMTIyMGFlOTE4OCIsInR5cCI6IkJlYXJlciIsImF6cCI6InBvcnRhbGUtYWdlYSIsIm5vbmNlIjoiNTk0ZGM5M2EtZDExYy00YjZjLTk4ZDgtMTc3YmJiMjU2YTI3Iiwic2Vzc2lvbl9zdGF0ZSI6IjYwNjIwYTJlLTFmNTktNDA3MC1iM2UxLTc2OGY5NDI3MmZkMCIsImFjciI6IjAiLCJhbGxvd2VkLW9yaWdpbnMiOlsiKiJdLCJyZWFsbV9hY2Nlc3MiOnsicm9sZXMiOlsiZGVmYXVsdC1yb2xlcy1hZ2VhX3Nzb19yZWFsbSIsIm9mZmxpbmVfYWNjZXNzIiwidW1hX2F1dGhvcml6YXRpb24iXX0sInJlc291cmNlX2FjY2VzcyI6eyJhY2NvdW50Ijp7InJvbGVzIjpbIm1hbmFnZS1hY2NvdW50IiwibWFuYWdlLWFjY291bnQtbGlua3MiLCJ2aWV3LXByb2ZpbGUiXX19LCJzY29wZSI6Im9wZW5pZCBlbWFpbCBwcm9maWxlIiwic2lkIjoiNjA2MjBhMmUtMWY1OS00MDcwLWIzZTEtNzY4Zjk0MjcyZmQwIiwidWlkIjoiYmdnc21uNzNoMDFkMDA3eSIsImVtYWlsX3ZlcmlmaWVkIjpmYWxzZSwibmFtZSI6IlNPTU4gQkFHRyIsInVzZXJUeXBlIjoiQUdSSSIsInByZWZlcnJlZF91c2VybmFtZSI6ImJnZ3NtbjczaDAxZDAwN3kiLCJmaXNjYWxfY29kZSI6IkJHR1NNTjczSDAxRDAwN1kiLCJnaXZlbl9uYW1lIjoiU09NTiIsImZhbWlseV9uYW1lIjoiQkFHRyIsImVtYWlsIjoic2lhbkBtYWlsLml0IiwibGFzdF9hY2Nlc3NfZGF0ZV90aW1lIjoiMjAyNS0xMC0wOCAxNTozMjo1MiJ9.I2JKgZCbZTBTtRIlI68CWiW2ohon6p3lZ0FHyd5zKKbFXOXIkb6m3ffuvUwSRarD-tTNkBJpRYmadJYOfXu2xRWPeBxzne7yAcvXJ56D372ZZgHfxbDGzjLFWznvFXS27HXxbRoqbNKQK5sVQue3Aclo6HzgX64cBFlfEsZYfYyn719rXoqcuVC5jV6bZ2ZS5JZTY_WAWKCOLxX-bVdQ6acHq7KrFm87T5D_f_8H7q4-LKdlAqliUUEC6GSLIv2f4v0p3M68X4LBKqh03ZoJergKIB5t0qpabQbKFow3EP5-yALo1me9bS8NQ8qPLTUIBG5g798FxfbZfMYZFRhfDg\" -H \"Content-Type: multipart/form-data\" -F \"dataAppuntamento=2025-10-09\" -F \"oraInizio=09:00\" -F \"idTipoRichiesta=1\" -F \"oraFine=10:00\" -F \"descrizione=Prova caricamento file via curl\" -F \"oggetto=Incontro tecnico\" -F \"allegati[0].nomeFile=Teams.pdf\" -F \"allegati[0].idTipoCaricamento=1\" -F \"allegati[0].file=@C:\\Users\\amaio\\Documents\\Progetti_Java\\Agea\\Teams\\Teams.pdf\" -F \"allegati[1].nomeFile=Documento.pdf\" -F \"allegati[1].idTipoCaricamento=2\" -F \"allegati[1].file=@C:\\Users\\amaio\\Documents\\pdf_di_test.pdf\"\r\n"
    	    ;
    	    
    public static final String INFO_FOR_UPDATE_APPOINTMENT =
    	    "Questa API richiede test tramite multipart/form-data con FormData; lo Swagger UI non funzionerà per questo tipo di richiesta.\n" +
    	    "Esempio di test tramite curl per l'invio di due file:\n\n" +
    	    "curl -X PUT \"http://localhost:8080/api/v1/agricoltore/updateAppuntamento/45\" -H \"X-CODE-CUAA: cua_01_prova\" -H \"Authorization: Bearer eyJhbGciOiJSUzI1NiIsInR5cCIgOiAiSldUIiwia2lkIiA6ICI1ZDA5WDRkenBRUGJNbnFCZWNDZnVDZ3pwSFd0eWduTnI0VzE4d0lrM0tJIn0.eyJleHAiOjE3NjAwMDIyNDAsImlhdCI6MTc2MDAwMTk0MCwianRpIjoiMGM2ZjFkMzUtZDFjZS00MGY2LThlZjItMDBkZjEyN2I3NzRhIiwiaXNzIjoiaHR0cHM6Ly9zc28tcHJlLmFnZWEuZ292Lml0L2F1dGgvcmVhbG1zL2FnZWFfc3NvX3JlYWxtIiwiYXVkIjoiYWNjb3VudCIsInN1YiI6IjFmMDE1ZGE2LWZkMjEtNDY4Yi1hZDEwLWFlMTIyMGFlOTE4OCIsInR5cCI6IkJlYXJlciIsImF6cCI6InBvcnRhbGUtYWdlYSIsIm5vbmNlIjoiNTk0ZGM5M2EtZDExYy00YjZjLTk4ZDgtMTc3YmJiMjU2YTI3Iiwic2Vzc2lvbl9zdGF0ZSI6IjYwNjIwYTJlLTFmNTktNDA3MC1iM2UxLTc2OGY5NDI3MmZkMCIsImFjciI6IjAiLCJhbGxvd2VkLW9yaWdpbnMiOlsiKiJdLCJyZWFsbV9hY2Nlc3MiOnsicm9sZXMiOlsiZGVmYXVsdC1yb2xlcy1hZ2VhX3Nzb19yZWFsbSIsIm9mZmxpbmVfYWNjZXNzIiwidW1hX2F1dGhvcml6YXRpb24iXX0sInJlc291cmNlX2FjY2VzcyI6eyJhY2NvdW50Ijp7InJvbGVzIjpbIm1hbmFnZS1hY2NvdW50IiwibWFuYWdlLWFjY291bnQtbGlua3MiLCJ2aWV3LXByb2ZpbGUiXX19LCJzY29wZSI6Im9wZW5pZCBlbWFpbCBwcm9maWxlIiwic2lkIjoiNjA2MjBhMmUtMWY1OS00MDcwLWIzZTEtNzY4Zjk0MjcyZmQwIiwidWlkIjoiYmdnc21uNzNoMDFkMDA3eSIsImVtYWlsX3ZlcmlmaWVkIjpmYWxzZSwibmFtZSI6IlNPTU4gQkFHRyIsInVzZXJUeXBlIjoiQUdSSSIsInByZWZlcnJlZF91c2VybmFtZSI6ImJnZ3NtbjczaDAxZDAwN3kiLCJmaXNjYWxfY29kZSI6IkJHR1NNTjczSDAxRDAwN1kiLCJnaXZlbl9uYW1lIjoiU09NTiIsImZhbWlseV9uYW1lIjoiQkFHRyIsImVtYWlsIjoic2lhbkBtYWlsLml0IiwibGFzdF9hY2Nlc3NfZGF0ZV90aW1lIjoiMjAyNS0xMC0wOCAxNTozMjo1MiJ9.I2JKgZCbZTBTtRIlI68CWiW2ohon6p3lZ0FHyd5zKKbFXOXIkb6m3ffuvUwSRarD-tTNkBJpRYmadJYOfXu2xRWPeBxzne7yAcvXJ56D372ZZgHfxbDGzjLFWznvFXS27HXxbRoqbNKQK5sVQue3Aclo6HzgX64cBFlfEsZYfYyn719rXoqcuVC5jV6bZ2ZS5JZTY_WAWKCOLxX-bVdQ6acHq7KrFm87T5D_f_8H7q4-LKdlAqliUUEC6GSLIv2f4v0p3M68X4LBKqh03ZoJergKIB5t0qpabQbKFow3EP5-yALo1me9bS8NQ8qPLTUIBG5g798FxfbZfMYZFRhfDg\" -H \"Content-Type: multipart/form-data\" -F \"dataAppuntamento=2025-10-09\" -F \"oraInizio=09:00\" -F \"idTipoRichiesta=1\" -F \"oraFine=10:00\" -F \"descrizione=Prova per aggiornamento appuntamento e di caricamento file via curl\" -F \"oggetto=Incontro tecnico\" -F \"allegati[0].nomeFile=Teams.pdf\" -F \"allegati[0].idTipoCaricamento=1\" -F \"allegati[0].file=@C:\\Users\\amaio\\Documents\\Progetti_Java\\Agea\\Teams\\Teams.pdf\" -F \"allegati[1].nomeFile=Documento.pdf\" -F \"allegati[1].idTipoCaricamento=2\" -F \"allegati[1].file=@C:\\Users\\amaio\\Documents\\pdf_di_test.pdf\"\r\n"
    	    ;
}
