package budgethanteraren;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CsvTransactionStoreTest {

    @TempDir
    Path tempDir;

    @Test
    void saveThenLoad_returnsSameTransactions() throws Exception {
        // Arrange
        CsvTransactionStore store = new CsvTransactionStore(tempDir.resolve("test.csv"));
        List<Transaction> original = List.of(
                new Transaction(LocalDate.of(2026, 9, 1), "Lön", 30000.0, TransactionType.INCOME),
                new Transaction(LocalDate.of(2026, 9, 3), "Mat", 249.50, TransactionType.EXPENSE));

        // Act
        store.save(original);
        List<Transaction> loaded = store.load();

        // Assert
        assertEquals(original, loaded);
    }

    @Test
    void load_missingFile_createsFileAndReturnsEmptyList() throws Exception {
        // Arrange
        Path path = tempDir.resolve("finns-inte.csv");
        CsvTransactionStore store = new CsvTransactionStore(path);

        // Act
        List<Transaction> result = store.load();

        // Assert
        assertTrue(result.isEmpty());
        assertTrue(Files.exists(path));
    }

    @Test
    void load_corruptLine_throwsFileFormatException() throws IOException {
        // Arrange
        Path path = tempDir.resolve("trasig.csv");
        Files.writeString(path, """
                datum;kategori;belopp;typ
                2026-09-01;Mat;abc;EXPENSE
                """, StandardCharsets.UTF_8);
        CsvTransactionStore store = new CsvTransactionStore(path);

        // Act + Assert
        FileFormatException exception =
                assertThrows(FileFormatException.class, store::load);

        assertTrue(exception.getMessage().contains("2"));
    }

    @Test
    void load_lineWithEmptyLastField_throwsFileFormatException() throws IOException {
        Path path = tempDir.resolve("tomt-fält.csv");
        Files.writeString(path, """
            datum;kategori;belopp;typ
            2026-09-01;Mat;100.00;
            """, StandardCharsets.UTF_8);
        CsvTransactionStore store = new CsvTransactionStore(path);

        assertThrows(FileFormatException.class, store::load);
    }

}