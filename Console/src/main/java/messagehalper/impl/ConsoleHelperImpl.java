package messagehalper.impl;

import messagehalper.ConsoleHelper;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

/**
 * Created by s.sergienko on 03.03.2017.
 */
public class ConsoleHelperImpl implements ConsoleHelper {
    private BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));

    @Override
    public synchronized void writeTextMessage(String textMessage) {
        System.out.println(textMessage);
    }

    @Override
    public synchronized void writeInfoMessage(String infoMessage) {
        System.out.println("\u001B[34m" + infoMessage + "\u001B[0m");
    }

    @Override
    public synchronized void writeErrorMessage(String errorMessage) {
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
                writeErrorMessage("Text input error. Please try again.");
            }
        }
    }

    @Override
    public int readInt() {
        while (true) {
            try {
                return Integer.parseInt(readString());
            }
            catch (NumberFormatException e) {
                writeErrorMessage("Number input error. Please try again.");
            }
        }
    }
}
