package us.hogu.exception;

public class ValidationException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final String errorCode;

    public ValidationException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public String getErrorCode() {
        return errorCode;
    }
}
