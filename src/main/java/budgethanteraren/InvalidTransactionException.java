package budgethanteraren;

import java.util.logging.Logger;

/**
 * Kastas när indata till en transaktion är ogiltig,
 * till exempel negativt belopp eller tom kategori.
 */
public class InvalidTransactionException extends RuntimeException {
    public InvalidTransactionException(String message) {
        super(message);
    }
}