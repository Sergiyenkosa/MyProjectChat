package project.chat.gui;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Created by s.sergienko on 19.10.2016.
 */
public class GuiModelClient {
    private final Set<String> userNames = new HashSet<>();
    private String newMessage;

    public Set<String> getAllUserNames() {
        return Collections.unmodifiableSet(userNames);
    }

    public void addUser(String newUserName) {
        userNames.add(newUserName);
    }

    public void deleteUser(String userName) {
        if (userNames.contains(userName)) {
            userNames.remove(userName);
        }
    }

    public String getNewMessage() {
        return newMessage;
    }

    public void setNewMessage(String newMessage) {
        this.newMessage = newMessage;
    }
}