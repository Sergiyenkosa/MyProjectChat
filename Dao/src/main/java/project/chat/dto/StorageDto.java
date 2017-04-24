package project.chat.dto;

/**
 * Created by Sergiy on 11.03.2017.
 */
public interface StorageDto {
    UserDto findByLogin(String login);
}
