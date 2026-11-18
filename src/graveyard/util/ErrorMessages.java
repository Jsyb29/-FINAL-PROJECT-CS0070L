package graveyard.util;

import graveyard.exception.CemeteryException;

public final class ErrorMessages {
    private ErrorMessages() { }

    /** Uses pattern matching to pick a message, 
    but keeps the original Throwable around for the developer's log. */
    
    public static String forUser(Throwable error) {
        if (error instanceof CemeteryException cemeteryError) return cemeteryError.getMessage();
        if (error instanceof InterruptedException) return "The operation was interrupted.";
        return "The operation failed. See the developer log for details.";
    }
}
