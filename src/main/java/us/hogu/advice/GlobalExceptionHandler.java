package us.hogu.advice;

import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.security.access.AccessDeniedException;

import us.hogu.common.constants.ErrorConstants;
import us.hogu.controller.dto.response.ErrorResponseDto;
import us.hogu.exception.ResourceNotFoundException;
import us.hogu.exception.ValidationException;

@RestControllerAdvice
public class GlobalExceptionHandler {
    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * Gestisce le eccezioni di accesso negato (403 Forbidden).
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponseDto> handleAccessDeniedException(AccessDeniedException ex) {
        ErrorResponseDto response = new ErrorResponseDto(
                ErrorConstants.ACCESS_DENIED.name(),
                ErrorConstants.ACCESS_DENIED.getMessage(),
                new Date()
        );
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
    }

    /**
     * Gestisce le eccezioni di autenticazione fallita (401 Unauthorized).
     */
    @ExceptionHandler(org.springframework.security.core.AuthenticationException.class)
    public ResponseEntity<ErrorResponseDto> handleAuthenticationException(org.springframework.security.core.AuthenticationException ex) {
        logger.warn("Autenticazione fallita", ex);
        ErrorResponseDto response = new ErrorResponseDto(
                ErrorConstants.UNAUTHORIZED.name(),
                ErrorConstants.UNAUTHORIZED.getMessage(),
                new Date()
        );
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
    }

    /**
     * Gestisce gli errori di validazione delle DTO annotate con @Valid.
     * Restituisce un messaggio unico concatenando tutti gli errori dei campi.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponseDto> handleMethodArgumentNotValidException(MethodArgumentNotValidException ex) {
        List<String> errors = ex.getBindingResult()
                                .getFieldErrors()
                                .stream()
                                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                                .collect(Collectors.toList());

        String message = String.join("; ", errors);

        ErrorResponseDto response = new ErrorResponseDto(
                ErrorConstants.VALIDATION_ERROR.name(),
                message,
                new Date()
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    /**
     * Gestisce le eccezioni di validazione custom definite nell'applicazione.
     */
    @ExceptionHandler(ValidationException.class)
    public ResponseEntity<ErrorResponseDto> handleValidationException(ValidationException ex) {
        ErrorResponseDto response = new ErrorResponseDto(
                ex.getErrorCode(),
                ex.getMessage(),
                new Date()
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }
    
    /**
     * Gestisce le eccezioni di validazione custom definite nell'applicazione.
     */
    @ExceptionHandler(BindException.class)
    public ResponseEntity<ErrorResponseDto> handleBindException(BindException ex) {
        ErrorResponseDto response = new ErrorResponseDto(
                ErrorConstants.PARAMS_NOT_VALID.name(),
                ErrorConstants.PARAMS_NOT_VALID.getMessage(),
                new Date()
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }
    
    /**
     * Gestisce le eccezioni di risorsa non trovata (404 Not Found).
     * Permette di mostrare il messaggio personalizzato passato da codice.
     */
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponseDto> handleResourceNotFoundException(ResourceNotFoundException ex) {
        ErrorResponseDto response = new ErrorResponseDto(
        		ErrorConstants.RESOURCE_NOT_FOUND.name(),
                ex.getMessage(),
                new Date()
        );
        
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
    }

    /**
     * Gestisce tutte le eccezioni non previste (errore generico 500).
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponseDto> handleGenericException(Exception ex) {
        logger.error("Errore generico non gestito", ex);
        ErrorResponseDto response = new ErrorResponseDto(
                ErrorConstants.GENERIC_ERROR.name(),
                ErrorConstants.GENERIC_ERROR.getMessage(),
                new Date()
        );
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }
    
    // TODO CREARE ERRORE PER TOKEN SCADUTO
}
