package messagehalper;

/**
 * Created by s.sergienko on 03.03.2017.
 */
public interface ConsoleHelper {
    void writeTextMessage(String textMessage);

    void writeInfoMessage(String infoMessage);

    void writeErrorMessage(String errorMessage);

    String readString();

    int readInt();
}
