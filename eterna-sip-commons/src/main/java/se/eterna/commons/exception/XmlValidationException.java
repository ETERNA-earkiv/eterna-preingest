package se.eterna.commons.exception;

public class XmlValidationException extends RuntimeException {

    public XmlValidationException(String message) {
        super(message);
    }

    public XmlValidationException(Throwable cause) {
        super("XML-validering misslyckades: " + cause.getMessage(), cause);
    }

    public XmlValidationException(String message, Throwable cause) {
        super(message, cause);
    }
}
