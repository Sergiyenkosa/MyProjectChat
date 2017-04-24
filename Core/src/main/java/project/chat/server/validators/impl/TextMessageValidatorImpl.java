package project.chat.server.validators.impl;

import project.chat.server.validators.TextMessageValidator;


/**
 * Created by s.sergienko on 18.01.2017.
 */
public class TextMessageValidatorImpl implements TextMessageValidator {
    private final int maxTextLength;

    public TextMessageValidatorImpl(int maxTextLength) {
        this.maxTextLength = maxTextLength;
    }

    @Override
    public boolean isTextMessageCorrect(String message) {
        return message.length() < maxTextLength;
    }

    @Override
    public int getMaxTextLength() {
        return maxTextLength;
    }
}
