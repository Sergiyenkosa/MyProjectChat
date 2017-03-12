package daos.impl;

import daos.StorageDao;
import daos.UserDao;
import models.User;
import store.Storage;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Created by Sergiy on 11.03.2017.
 */
public class StorageDaoImpl implements StorageDao{
    private final Storage<User> storage;

    public StorageDaoImpl(Storage<User> storage) {
        this.storage = storage;
    }

    public Collection<UserDao> values() {
        List<UserDao> userDaos = new ArrayList<>();

        for (User user : storage.values()) {
            userDaos.add(new UserDao(user.getId(), user.getLogin(), user.getPassword()));
        }

        return userDaos;
    }

    public int add(final UserDao user) {
        return storage.add(new User(user.getId(), user.getLogin(), user.getPassword()));
    }

    public void delete(int id) {
        storage.delete(id);
    }

    public UserDao get(int id) {
        User user = storage.get(id);

        return new UserDao(user.getId(), user.getLogin(), user.getPassword());
    }

    public UserDao findByLogin(String login) {
        User user = storage.findByLogin(login);

        return user != null ? new UserDao(user.getId(), user.getLogin(), user.getPassword()) : null;
    }
}
