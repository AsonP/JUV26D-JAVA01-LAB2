package budgethanteraren;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import java.util.logging.Logger;


/**
 * Generiskt lager för objekt av valfri typ T.
 *
 * @param <T> typen av objekt som lagras
 */
public class Repository<T> {
    private static final Logger logger = Logger.getLogger(Repository.class.getName());
    private final List<T> items = new ArrayList<>();

    public void add(T item) {
        if (item == null) {
            logger.warning("Försök att lagra null avvisat.");
            throw new IllegalArgumentException("Kan inte lagra null.");
        }
        items.add(item);
    }

    public List<T> findAll() {
       return new ArrayList<>(items);
    }

    //public List<T> findAll() { //Ger fel i tester
       // return items;
   // }

    public List<T> findWhere(Predicate<T> condition) {
        return items.stream()
                .filter(condition)
                .toList();
    }

    public int size() {
        return items.size();
    }
}