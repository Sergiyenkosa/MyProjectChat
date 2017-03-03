package messagehandlers.impl;

import messagehandlers.TextMessageHelper;

/**
 * Created by s.sergienko on 03.03.2017.
 */
public class TextMessageConsoleHelper implements TextMessageHelper{
    @Override
    public void writeTextMessage(String textMessage) {
        System.out.println(textMessage);
    }

    @Override
    public void writeInfoMessage(String infoMessage) {
        System.out.println(infoMessage);
    }

    @Override
    public void writeProgramMessage(String programMessage) {
        System.out.println(programMessage);
    }

    @Override
    public void writeErrorMessage(String errorMessage) {
        System.out.println(errorMessage);
    }

    @Override
    public String readString() {
        return null;
    }

    @Override
    public int readInt() {
        return 0;
    }
}
