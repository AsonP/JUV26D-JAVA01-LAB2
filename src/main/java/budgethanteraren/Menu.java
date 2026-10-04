package budgethanteraren;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Scanner;
import java.util.logging.Logger;

public class Menu {

    private static final Logger logger = Logger.getLogger(Menu.class.getName());

    private final BudgetService service;
    private final CsvTransactionStore store;
    private final Scanner scanner = new Scanner(System.in);

    public Menu(BudgetService service, CsvTransactionStore store) {
        this.service = Objects.requireNonNull(service, "service får inte vara null");
        this.store = Objects.requireNonNull(store, "store får inte vara null");
    }

    // --- Huvudloop --------------------------------------------------------

    public void run() {
        loadFromFile();
        logger.info("Applikationen startad.");

        boolean running = true;

        while (running) {
            printMenu();
            String choice = scanner.nextLine().trim().toLowerCase();

            switch (choice) {
                case "1" -> addTransaction();
                case "2" -> showAllTransactions();
                case "3" -> showSummary();
                case "4" -> filterMenu();
                case "5" -> saveToFile();
                case "e" -> {
                    saveToFile();
                    running = false;
                }
                default -> System.out.println("Ogiltigt val. Försök igen.");
            }
        }

        logger.info("Applikationen avslutas.");
        System.out.println("Avslutar Budgethanteraren.");
    }

    private void printMenu() {
        System.out.println("""

                Budgethanteraren
                ================
                1. Lägg till transaktion
                2. Visa alla transaktioner
                3. Visa saldo och sammanställning per kategori
                4. Filtrera transaktioner
                5. Spara till fil
                e. Avsluta""");
        System.out.print("Val: ");
    }

    // --- Menyval ----------------------------------------------------------

    private void addTransaction() {
        try {
            LocalDate date = readDate("Datum (ÅÅÅÅ-MM-DD, tomt = idag): ");
            String category = readNonEmpty("Kategori: ");
            double amount = readAmount("Belopp (positivt tal): ");
            TransactionType type = readType();

            service.add(new Transaction(date, category, amount, type));
            System.out.println("Transaktionen lades till.");

        } catch (InvalidTransactionException e) {
            logger.warning("Ogiltig transaktion avvisad: " + e.getMessage());
            System.out.println("Kunde inte lägga till transaktionen: " + e.getMessage());
        }
    }

    private void showAllTransactions() {
        print(service.findAll());
    }

    private void showSummary() {
        System.out.printf("%nSaldo: %.2f kr%n", service.balance());

        Map<String, Double> perCategory = service.sumPerCategory();
        System.out.println("\nPer kategori:");

        if (perCategory.isEmpty()) {
            System.out.println("  (inga transaktioner)");
            return;
        }
        perCategory.forEach((category, sum) ->
                System.out.printf("  %-20s %12.2f kr%n", category, sum));
    }

    private void filterMenu() {
        System.out.println("""

                Filtrera
                --------
                1. Per typ
                2. Per datumintervall
                0. Tillbaka""");
        System.out.print("Val: ");

        switch (scanner.nextLine().trim()) {
            case "1" -> print(service.findByType(readType()));
            case "2" -> filterByDateRange();
            case "0" -> { }
            default -> System.out.println("Ogiltigt val.");
        }
    }

    private void filterByDateRange() {
        LocalDate from = readDate("Från och med (ÅÅÅÅ-MM-DD, tomt = idag): ");
        LocalDate to = readDate("Till och med (ÅÅÅÅ-MM-DD, tomt = idag): ");

        if (to.isBefore(from)) {
            logger.warning("Bakvänt datumintervall avvisat: " + from + " till " + to);
            System.out.println("Slutdatumet ligger före startdatumet.");
            return;
        }
        print(service.findBetweenDates(from, to));
    }

    // --- Fil --------------------------------------------------------------

    private void loadFromFile() {
        try {
            List<Transaction> loaded = store.load();
            loaded.forEach(service::add);
            System.out.printf("Läste in %d transaktion(er) från %s.%n",
                    loaded.size(), store.getPath());

        } catch (FileFormatException e) {
            logger.severe("Inläsning misslyckades: " + e.getMessage());
            System.out.println("Filen kunde inte tolkas: " + e.getMessage());
            System.out.println("Startar med tom budget. Filen skrivs över först när du sparar.");

        } catch (IOException e) {
            logger.severe("Inläsning misslyckades: " + e.getMessage());
            System.out.println("Kunde inte läsa filen: " + e.getMessage());
        }
    }

    private void saveToFile() {
        try {
            store.save(service.findAll());
            System.out.printf("Sparade %d transaktion(er) till %s.%n",
                    service.findAll().size(), store.getPath());

        } catch (IOException e) {
            logger.severe("Sparning misslyckades: " + e.getMessage());
            System.out.println("Kunde inte spara: " + e.getMessage());
        }
    }

    // --- Utskrift ---------------------------------------------------------

    private void print(List<Transaction> transactions) {
        if (transactions.isEmpty()) {
            System.out.println("\n(inga transaktioner att visa)");
            return;
        }
        System.out.printf("%n%-12s %-20s %12s  %s%n", "Datum", "Kategori", "Belopp", "Typ");
        for (Transaction t : transactions) {
            System.out.printf("%-12s %-20s %12.2f  %s%n",
                    t.date(), t.category(), t.signedAmount(), t.type().getLabel());
        }
    }

    // --- Inläsning med validering ----------------------------------------

    private LocalDate readDate(String prompt) {
        while (true) {
            System.out.print(prompt);
            String input = scanner.nextLine().trim();

            if (input.isEmpty()) {
                return LocalDate.now();
            }
            try {
                return LocalDate.parse(input);
            } catch (DateTimeParseException e) {
                logger.warning("Ogiltigt datum avvisat: " + input);
                System.out.println("Ogiltigt datum. Använd formatet ÅÅÅÅ-MM-DD.");
            }
        }
    }

    private String readNonEmpty(String prompt) {
        while (true) {
            System.out.print(prompt);
            String input = scanner.nextLine().trim();

            if (!input.isBlank()) {
                return input;
            }
            logger.warning("Tomt värde avvisat.");
            System.out.println("Värdet får inte vara tomt.");
        }
    }

    private double readAmount(String prompt) {
        while (true) {
            System.out.print(prompt);
            String input = scanner.nextLine().trim().replace(',', '.');

            try {
                double value = Double.parseDouble(input);

                if (value <= 0) {
                    logger.warning("Ogiltigt belopp avvisat: " + input);
                    System.out.println("Beloppet måste vara större än noll. "
                            + "En utgift markeras med typ, inte med minustecken.");
                    continue;
                }
                return value;

            } catch (NumberFormatException e) {
                logger.warning("Ogiltigt tal avvisat: " + input);
                System.out.println("Ogiltigt tal. Exempel: 249,50");
            }
        }
    }

    private TransactionType readType() {
        while (true) {
            System.out.print("Typ (i = inkomst, u = utgift): ");
            String input = scanner.nextLine().trim().toLowerCase();

            switch (input) {
                case "i", "inkomst" -> {
                    return TransactionType.INCOME;
                }
                case "u", "utgift" -> {
                    return TransactionType.EXPENSE;
                }
                default -> {
                    logger.warning("Ogiltig transaktionstyp avvisad: " + input);
                    System.out.println("Ange i eller u.");
                }
            }
        }
    }
}