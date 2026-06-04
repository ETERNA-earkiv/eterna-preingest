package se.eterna.commons.exception;

public class EternaClientException extends RuntimeException {

    public EternaClientException(String message) {
        super(message);
    }

    public EternaClientException(String message, Throwable cause) {
        super(message, cause);
    }
}
