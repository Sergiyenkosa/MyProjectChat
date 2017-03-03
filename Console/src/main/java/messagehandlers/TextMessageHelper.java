package messagehandlers;

/**
 * Created by s.sergienko on 03.03.2017.
 */
public interface TextMessageHelper {
    void writeTextMessage(String textMessage);

    void writeInfoMessage(String infoMessage);

    void writeProgramMessage(String programMessage);

    void writeErrorMessage(String errorMessage);

    String readString();

    int readInt();
}
