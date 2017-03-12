package store;

import java.util.Collection;

/**
 * Created by Sergiy on 09.03.2017.
 */
public interface Storage<T> {
    Collection<T> values();

    int add(final T user);

    void delete(int id);

    T get(int id);

    T findByLogin(String login);
}
