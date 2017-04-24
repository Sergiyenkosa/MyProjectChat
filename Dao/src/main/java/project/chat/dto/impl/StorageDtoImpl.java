package project.chat.dto.impl;

import project.chat.entity.Storage;
import project.chat.entity.User;
import project.chat.dto.StorageDto;
import project.chat.dto.UserDto;

/**
 * Created by Sergiy on 11.03.2017.
 */
public class StorageDtoImpl implements StorageDto {
    private final Storage<User> storage;

    public StorageDtoImpl(Storage<User> storage) {
        this.storage = storage;
    }

    public UserDto findByLogin(String login) {
        User user = storage.findByLogin(login);

        return user != null ? new UserDto(user.getLogin(), user.getPassword()) : null;
    }
}
