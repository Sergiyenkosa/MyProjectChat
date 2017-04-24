package project.chat.entity;

/**
 * Created by Sergiy on 09.03.2017.
 */
public interface Storage<T> {
    T findByLogin(String login);
}
