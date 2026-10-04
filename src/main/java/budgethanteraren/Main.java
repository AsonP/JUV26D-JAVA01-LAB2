package budgethanteraren;

import java.nio.file.Path;
import java.util.logging.Logger;

public class Main {

    private static final Logger logger = Logger.getLogger(Main.class.getName());

    private static final String DEFAULT_FILE = "data/transaktioner.csv";

    public static void main(String[] args) {
        Path dataFile = Path.of(args.length > 0 ? args[0] : DEFAULT_FILE);
        logger.fine("Startar med datafil: " + dataFile);

        Repository<Transaction> repository = new Repository<>();
        BudgetService service = new BudgetService(repository);
        CsvTransactionStore store = new CsvTransactionStore(dataFile);

        new Menu(service, store).run();
    }
}