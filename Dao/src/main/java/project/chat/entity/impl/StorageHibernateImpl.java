package project.chat.entity.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.orm.hibernate5.HibernateTemplate;
import org.springframework.stereotype.Repository;
import project.chat.entity.Storage;
import project.chat.entity.User;

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
    public User findByLogin(String login) {
        List<User> user = (List<User>) template.find("from User as user where user.login='" + login + "'");
        if (user.isEmpty()) {
            return null;
        } else {
            return user.get(0);
        }
    }
}
