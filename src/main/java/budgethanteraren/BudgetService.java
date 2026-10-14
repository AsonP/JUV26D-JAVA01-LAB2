package budgethanteraren;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.TreeMap;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * All affärslogik kring budgeten. Känner inte till hur data visas eller
 * var den lagras – bara vad som ska räknas ut.
 */
public class BudgetService {

    private static final Logger logger = Logger.getLogger(BudgetService.class.getName());

    private final Repository<Transaction> repository;

    public BudgetService(Repository<Transaction> repository) {
        this.repository = Objects.requireNonNull(repository, "repository får inte vara null");
    }

    // --- Lägga till och hämta --------------------------------------------

    public void add(Transaction transaction) {
        repository.add(transaction);
        logger.fine("Transaktion tillagd: " + transaction);
    }

    public List<Transaction> findAll() {
        return repository.findAll();
    }

    /**
     * Senaste transaktionen enligt datum, eller en tom Optional om inga finns.
     * Returnerar aldrig null – anroparen tvingas hantera båda fallen.
     */
    public Optional<Transaction> latest() {
        return repository.findAll().stream()
                .max(Comparator.comparing(Transaction::date));
    }

    // --- Sammanställning --------------------------------------------------

    /** Saldo = summan av alla inkomster minus alla utgifter. */
    public double balance() {
        return repository.findAll().stream()
                .mapToDouble(Transaction::signedAmount)
                .sum();
    }

    /** Nettosumma per kategori, sorterad på kategorinamn. */
    public Map<String, Double> sumPerCategory() {
        return repository.findAll().stream()
                .collect(Collectors.groupingBy(
                        Transaction::category,
                        TreeMap::new,
                        Collectors.summingDouble(Transaction::signedAmount)));
    }

    // --- Filtrering -------------------------------------------------------

    public List<Transaction> findByType(TransactionType type) {
        return repository.findWhere(t -> t.type() == type);
    }

    /** Inklusive både från- och till-datum. */
    public List<Transaction> findBetweenDates(LocalDate from, LocalDate to) {
        return repository.findWhere(t -> !t.date().isBefore(from) && !t.date().isAfter(to));
    }
}
