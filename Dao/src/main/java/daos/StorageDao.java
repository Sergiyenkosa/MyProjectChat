package daos;

import java.util.Collection;

/**
 * Created by Sergiy on 11.03.2017.
 */
public interface StorageDao {
    Collection<UserDao> values();

    int add(final UserDao user);

    void delete(int id);

    UserDao get(int id);

    UserDao findByLogin(String login);
}
