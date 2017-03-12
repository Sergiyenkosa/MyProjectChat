package store.impl;

import models.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.orm.hibernate5.HibernateTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import store.Storage;

import java.util.Collection;
import java.util.List;

/**
 * Created by Sergiy on 09.03.2017.
 */
@Repository
public class StorageHibernateImpl implements Storage<User> {
    private final HibernateTemplate template;

    @Autowired
    public StorageHibernateImpl(HibernateTemplate template) {
        this.template = template;
    }

    @Override
    public Collection<User> values() {
        return (List<User>) template.find("from User");
    }

    @Transactional
    @Override
    public int add(User user) {
        int uid = (int) template.save(user);
        user.setId(uid);

        return uid;
    }

    @Transactional
    @Override
    public void delete(int id) {
        template.delete(new User(id, null, null));
    }

    @Override
    public User get(int id) {
        return template.get(User.class, id);
    }

    @Override
    public User findByLogin(String login) {
        List<User> user = (List<User>) template.find("from User as user where user.login='" + login + "'");
        if (user.isEmpty()) {
            return null;
        } else {
            return user.get(0);
        }
    }
}
