package se.eterna.commons.exception;

public class SipBuildException extends RuntimeException {

    public SipBuildException(String message) {
        super(message);
    }

    public SipBuildException(String message, Throwable cause) {
        super(message, cause);
    }
}
