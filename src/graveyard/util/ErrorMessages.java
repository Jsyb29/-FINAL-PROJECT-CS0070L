package graveyard.util;

import graveyard.exception.CemeteryException;

public final class ErrorMessages {
    private ErrorMessages() { }

    /** M5 pattern matching; keep the original Throwable for the developer's log. */
    public static String forUser(Throwable error) {
        if (error instanceof CemeteryException cemeteryError) return cemeteryError.getMessage();
        if (error instanceof InterruptedException) return "The operation was interrupted.";
        return "The operation failed. See the developer log for details.";
    }
}
