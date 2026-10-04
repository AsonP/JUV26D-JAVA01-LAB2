package budgethanteraren;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.logging.Logger;

/**
 * Läser och skriver transaktioner som semikolonseparerad textfil.
 *
 * Format: datum;kategori;belopp;typ
 * Exempel: 2026-09-01;Mat;249.50;EXPENSE
 */
public class CsvTransactionStore {

    private static final Logger logger = Logger.getLogger(CsvTransactionStore.class.getName());

    private static final String DELIMITER = ";";
    private static final String HEADER = "datum;kategori;belopp;typ";

    private final Path path;

    public CsvTransactionStore(Path path) {
        this.path = path;
    }

    public Path getPath() {
        return path;
    }

    // --- Läsning ----------------------------------------------------------

    public List<Transaction> load() throws IOException, FileFormatException {
        if (Files.notExists(path)) {
            logger.warning("Datafilen saknas, skapar ny: " + path);
            createEmptyFile();
            return List.of();
        }

        List<Transaction> result = new ArrayList<>();

        try (BufferedReader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            String line;
            int lineNumber = 0;

            while ((line = reader.readLine()) != null) {
                lineNumber++;

                if (line.isBlank()) {
                    continue;
                }
                if (lineNumber == 1 && line.startsWith("datum")) {
                    continue;
                }
                result.add(parseLine(line, lineNumber));
            }
        }

        logger.info("Läste in " + result.size() + " transaktioner från " + path);
        return result;
    }

    private Transaction parseLine(String line, int lineNumber) throws FileFormatException {
        String[] parts = line.split(DELIMITER, -1);

        if (parts.length != 4) {
            logger.severe("Rad " + lineNumber + " har fel antal fält: " + line);
            throw new FileFormatException(
                    "Rad " + lineNumber + " har " + parts.length + " fält, förväntade 4: " + line);
        }
        try {
            LocalDate date = LocalDate.parse(parts[0].trim());
            String category = parts[1].trim();
            double amount = Double.parseDouble(parts[2].trim());
            TransactionType type = TransactionType.valueOf(parts[3].trim().toUpperCase(Locale.ROOT));

            logger.fine("Tolkade rad " + lineNumber + ": " + line);
            return new Transaction(date, category, amount, type);

        } catch (DateTimeParseException | IllegalArgumentException | InvalidTransactionException e) {
            logger.severe("Kunde inte tolka rad " + lineNumber + ": " + line);
            throw new FileFormatException("Trasig rad " + lineNumber + ": " + line, e);
        }
    }

    // --- Skrivning --------------------------------------------------------

    public void save(List<Transaction> transactions) throws IOException {
        createParentDirectories();

        try (BufferedWriter writer = Files.newBufferedWriter(path,
                StandardCharsets.UTF_8,
                StandardOpenOption.CREATE,
                StandardOpenOption.WRITE,
                StandardOpenOption.TRUNCATE_EXISTING)) {

            writer.write(HEADER);
            writer.newLine();

            for (Transaction t : transactions) {
                writer.write(toLine(t));
                writer.newLine();
            }
        }

        logger.info("Sparade " + transactions.size() + " transaktioner till " + path);
    }

    private String toLine(Transaction t) {
        return String.join(DELIMITER,
                t.date().toString(),
                t.category(),
                String.format(Locale.ROOT, "%.2f", t.amount()),
                t.type().name());
    }

    // --- Filsystem --------------------------------------------------------

    private void createEmptyFile() throws IOException {
        createParentDirectories();
        Files.writeString(path, HEADER + System.lineSeparator(), StandardCharsets.UTF_8);
        logger.info("Skapade ny tom datafil: " + path);
    }

    private void createParentDirectories() throws IOException {
        Path parent = path.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
    }
}