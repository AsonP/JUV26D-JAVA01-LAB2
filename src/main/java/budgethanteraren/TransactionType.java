package budgethanteraren;

import java.util.logging.Logger;

public enum TransactionType {

    INCOME("Inkomst", 1),
    EXPENSE("Utgift", -1);

    private final String label;
    private final int sign;

    TransactionType(String label, int sign) {
        this.label = label;
        this.sign = sign;
    }

    public String getLabel() {
        return label;
    }

    public int getSign() {
        return sign;
    }

    /** Tillämpar typens tecken på ett positivt belopp. */
    public double applySign(double belopp) {
        return belopp * sign;
    }
}