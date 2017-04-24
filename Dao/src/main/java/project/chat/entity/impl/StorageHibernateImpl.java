package project.chat.entity.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.orm.hibernate5.HibernateTemplate;
import org.springframework.stereotype.Repository;
import project.chat.entity.User;
import project.chat.entity.Storage;

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
        return template.get(User.class, login);
    }
}
