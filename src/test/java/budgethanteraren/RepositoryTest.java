package budgethanteraren;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class RepositoryTest {

    @Test
    void add_singleTransaction_increasesSizeToOne() {
        // Arrange
        Repository<Transaction> repository = new Repository<>();
        Transaction transaction = new Transaction(
                LocalDate.of(2026, 9, 1), "Mat", 249.50, TransactionType.EXPENSE);

        // Act
        repository.add(transaction);

        // Assert
        assertEquals(1, repository.size());
    }

    @Test
    void add_null_throwsIllegalArgumentException() {
        // Arrange
        Repository<Transaction> repository = new Repository<>();

        // Act + Assert
        assertThrows(IllegalArgumentException.class, () -> repository.add(null));
    }


    @Test
    void findAll_modifyingResult_doesNotAffectRepository() {
        // Arrange
        Repository<Transaction> repository = new Repository<>();
        repository.add(new Transaction(
                LocalDate.of(2026, 9, 1), "Mat", 249.50, TransactionType.EXPENSE));

        // Act
        List<Transaction> result = repository.findAll();
        result.clear();

        // Assert
        assertEquals(1, repository.size());
    }







}
