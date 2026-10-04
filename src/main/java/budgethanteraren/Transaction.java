package budgethanteraren;

import java.time.LocalDate;
import java.util.logging.Logger;

public record Transaction(LocalDate date, String category,
                          double amount, TransactionType type) {

    public Transaction {
        if (date == null) {
            throw new InvalidTransactionException("Datum måste anges.");
        }
        if (category == null || category.isBlank()) {
            throw new InvalidTransactionException("Kategori får inte vara tom.");
        }
        if (type == null) {
            throw new InvalidTransactionException("Transaktionstyp måste anges.");
        }
        if (amount <= 0) {
            throw new InvalidTransactionException(
                    "Beloppet måste vara större än noll (angavs: " + amount + ").");
        }
        category = category.trim();
    }

    public double signedAmount() {
        return type.applySign(amount);
    }
}