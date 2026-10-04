package budgethanteraren;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.logging.Logger;
import java.util.stream.Collectors;



public class BudgetService {
    private static final Logger logger = Logger.getLogger(BudgetService.class.getName());

    private final Repository<Transaction> repository;

    public BudgetService(Repository<Transaction> repository) {
        this.repository = Objects.requireNonNull(repository, "repository får inte vara null");
    }

    public double balance() {
        return repository.findAll().stream()
                .mapToDouble(Transaction::signedAmount)
                .sum();
    }

    public Map<String, Double> sumPerCategory() {
        return repository.findAll().stream()
                .collect(Collectors.groupingBy(
                        Transaction::category,
                        TreeMap::new,
                        Collectors.summingDouble(Transaction::signedAmount)));
    }

    public List<Transaction> findByType(TransactionType type) {
        return repository.findWhere(t -> t.type() == type);
    }

    public List<Transaction> findBetweenDates(LocalDate from, LocalDate to) {
        return repository.findWhere(t -> !t.date().isBefore(from) && !t.date().isAfter(to));
    }

    public List<Transaction> findAll() {
        return repository.findAll();
    }

    public void add(Transaction transaction) {
        repository.add(transaction);
        logger.fine("Transaktion tillagd: " + transaction);


    }


}
