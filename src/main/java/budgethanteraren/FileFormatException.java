package budgethanteraren;

import java.util.logging.Logger;

/**
 * Kastas när en rad i datafilen inte går att tolka.
 */
public class FileFormatException extends Exception {
    public FileFormatException(String message) {
        super(message);
    }

    public FileFormatException(String message, Throwable cause) {
        super(message, cause);
    }
}


