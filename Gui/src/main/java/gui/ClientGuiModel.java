package gui;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Created by s.sergienko on 19.10.2016.
 */
public class ClientGuiModel {
    private final Set<String> allUserNames = new HashSet<>();
    private String newMessage;

    public Set<String> getAllUserNames() {
        return Collections.unmodifiableSet(allUserNames);
    }

    public void addUser(String newUserName) {
        allUserNames.add(newUserName);
    }

    public void deleteUser(String userName) {
        if (allUserNames.contains(userName)) {
            allUserNames.remove(userName);
        }
    }

    public String getNewMessage() {
        return newMessage;
    }

    public void setNewMessage(String newMessage) {
        this.newMessage = newMessage;
    }
}