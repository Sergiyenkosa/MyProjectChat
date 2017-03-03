package server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

/**
 * Created by s.sergienko on 11.10.2016.
 */
public class ConsoleHelper {
    private static BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));

    public static void writeMessage(String message) {
        System.out.println(message);
    }

    public static String readString() {
        while (true) {
            try {
                String text = reader.readLine();

                if(System.getProperty("os.name").contains("Windows")) {
                    return new String(text.getBytes(), "utf8");
                }

                return text;
            }
            catch (IOException e) {
                writeMessage("Произошла ошибка при попытке ввода текста. Попробуйте еще раз.");
            }
        }
    }

    public static int readInt() {
        while (true) {
            try {
                writeMessage("Введите порт сервера");
                return Integer.parseInt(readString());
            }
            catch (NumberFormatException e) {
                writeMessage("Произошла ошибка при попытке ввода числа. Попробуйте еще раз.");
            }
        }
    }
}
