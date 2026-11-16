package graveyard.exception;

/** Keeps the original cause for debugging; the UI may display the safe message. */
public class CemeteryException extends RuntimeException {
    private static final long serialVersionUID = 1L;
    public CemeteryException(String message) { super(message); }
    public CemeteryException(String message, Throwable cause) { super(message, cause); }
}
