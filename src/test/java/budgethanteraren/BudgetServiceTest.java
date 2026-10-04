package budgethanteraren;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;



class BudgetServiceTest {

    @Test
    void balance_noTransactions_returnsZero() {
        // Arrange
        Repository<Transaction> repository = new Repository<>();
        BudgetService service = new BudgetService(repository);

        // Act
        double result = service.balance();

        // Assert
        assertEquals(0.0, result, 0.0001);
    }

    @Test
    void balance_incomeAndExpense_returnsDifference() {
        // Arrange
        Repository<Transaction> repository = new Repository<>();
        repository.add(new Transaction(
                LocalDate.of(2026, 9, 1), "Lön", 30000.0, TransactionType.INCOME));
        repository.add(new Transaction(
                LocalDate.of(2026, 9, 3), "Mat", 500.0, TransactionType.EXPENSE));
        BudgetService service = new BudgetService(repository);

        // Act
        double result = service.balance();

        // Assert
        assertEquals(29500.0, result, 0.0001);
    }

    @Test
    void sumPerCategory_noTransactions_returnsEmptyMap() {
        // Arrange
        Repository<Transaction> repository = new Repository<>();
        BudgetService service = new BudgetService(repository);

        // Act
        Map<String, Double> result = service.sumPerCategory();

        // Assert
        assertTrue(result.isEmpty());
    }

    @Test
    void sumPerCategory_multipleCategories_sumsEachSeparately() {
        // Arrange
        Repository<Transaction> repository = new Repository<>();
        repository.add(new Transaction(
                LocalDate.of(2026, 9, 1), "Mat", 300.0, TransactionType.EXPENSE));
        repository.add(new Transaction(
                LocalDate.of(2026, 9, 2), "Mat", 200.0, TransactionType.EXPENSE));
        repository.add(new Transaction(
                LocalDate.of(2026, 9, 3), "Lön", 30000.0, TransactionType.INCOME));
        BudgetService service = new BudgetService(repository);

        // Act
        Map<String, Double> result = service.sumPerCategory();

        // Assert
        assertEquals(2, result.size());
        assertEquals(-500.0, result.get("Mat"), 0.0001);
        assertEquals(30000.0, result.get("Lön"), 0.0001);
    }

    @Test
    void sumPerCategory_categoryNetsToZero_isStillIncluded() {
        // Arrange
        Repository<Transaction> repository = new Repository<>();
        repository.add(new Transaction(
                LocalDate.of(2026, 9, 1), "Resa", 2000.0, TransactionType.EXPENSE));
        repository.add(new Transaction(
                LocalDate.of(2026, 9, 5), "Resa", 2000.0, TransactionType.INCOME));
        BudgetService service = new BudgetService(repository);

        // Act
        Map<String, Double> result = service.sumPerCategory();

        // Assert
        assertEquals(1, result.size());
        assertEquals(0.0, result.get("Resa"), 0.0001);


    }

    @Test
    void findByType_mixedTransactions_returnsOnlyMatchingType() {
        // Arrange
        Repository<Transaction> repository = new Repository<>();
        repository.add(new Transaction(
                LocalDate.of(2026, 9, 1), "Mat", 300.0, TransactionType.EXPENSE));
        repository.add(new Transaction(
                LocalDate.of(2026, 9, 2), "Lön", 30000.0, TransactionType.INCOME));
        repository.add(new Transaction(
                LocalDate.of(2026, 9, 3), "Hyra", 8000.0, TransactionType.EXPENSE));
        BudgetService service = new BudgetService(repository);

        // Act
        List<Transaction> result = service.findByType(TransactionType.EXPENSE);

        // Assert
        assertEquals(2, result.size());
        assertTrue(result.stream().allMatch(t -> t.type() == TransactionType.EXPENSE));


    }

    @Test
    void findBetweenDates_transactionsOnBoundaries_areIncluded() {
        // Arrange
        Repository<Transaction> repository = new Repository<>();
        repository.add(new Transaction(
                LocalDate.of(2026, 8, 31), "Före", 100.0, TransactionType.EXPENSE));
        repository.add(new Transaction(
                LocalDate.of(2026, 9, 1), "Första dagen", 100.0, TransactionType.EXPENSE));
        repository.add(new Transaction(
                LocalDate.of(2026, 9, 15), "Mitten", 100.0, TransactionType.EXPENSE));
        repository.add(new Transaction(
                LocalDate.of(2026, 9, 30), "Sista dagen", 100.0, TransactionType.EXPENSE));
        repository.add(new Transaction(
                LocalDate.of(2026, 10, 1), "Efter", 100.0, TransactionType.EXPENSE));
        BudgetService service = new BudgetService(repository);

        // Act
        List<Transaction> result = service.findBetweenDates(
                LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 30));

        // Assert
        assertEquals(3, result.size());
    }

}
