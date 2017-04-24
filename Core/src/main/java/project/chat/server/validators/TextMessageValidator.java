package project.chat.server.validators;

/**
 * Created by s.sergienko on 18.01.2017.
 */
public interface TextMessageValidator {
    int getMaxTextLength();
    boolean isTextMessageCorrect(String message);
}
