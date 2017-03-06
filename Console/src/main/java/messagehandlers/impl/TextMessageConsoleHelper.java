package messagehandlers.impl;

import messagehandlers.TextMessageHelper;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

/**
 * Created by s.sergienko on 03.03.2017.
 */
public class TextMessageConsoleHelper implements TextMessageHelper{
    private BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));

    @Override
    public void writeTextMessage(String textMessage) {
        System.out.println(textMessage);
    }

    @Override
    public void writeInfoMessage(String infoMessage) {
        System.out.println("\u001B[34m" + infoMessage + "\u001B[0m");
    }

    @Override
    public void writeErrorMessage(String errorMessage) {
        System.out.println("\u001B[31m" + errorMessage + "\u001B[0m");
    }

    @Override
    public String readString() {
        while (true) {
            try {
                String text = reader.readLine();

                if(System.getProperty("os.name").contains("Windows")) {
                    return new String(text.getBytes(), "utf8");
                }

                return text;
            }
            catch (IOException e) {
                writeErrorMessage("Произошла ошибка при попытке ввода текста. Попробуйте еще раз.");
            }
        }
    }

    @Override
    public int readInt() {
        while (true) {
            try {
                writeInfoMessage("Введите порт сервера");
                return Integer.parseInt(readString());
            }
            catch (NumberFormatException e) {
                writeErrorMessage("Произошла ошибка при попытке ввода числа. Попробуйте еще раз.");
            }
        }
    }
}
