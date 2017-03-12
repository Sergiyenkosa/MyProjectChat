package console;


import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Created by s.sergienko on 18.10.2016.
 */
public class BotClient extends ConsoleClient{
    private static List<Integer> namesAndIndex = new ArrayList<>();

    @Override
    protected SocketThread getSocketThread() {
        return new BotSocketThread();
    }

    @Override
    public void run() {
        getSocketThread().run();
    }

    @Override
    protected boolean shouldSentTextFromConsole() {
        return false;
    }

    @Override
    protected String getUserName() {
        while (true) {
            Integer nameAndIndex = ThreadLocalRandom.current().nextInt(100);
            if (!namesAndIndex.contains(nameAndIndex)) {
                return "date_bot_" + nameAndIndex.toString();
            }
            else if (namesAndIndex.size() == 100) {
                return "date_bot_over 99";
            }
        }
    }

    @Override
    protected boolean askGetFile(String senderName, String fileName) {
        return false;
    }

    public static void main(String[] args) {
        new BotClient().run();
    }

    public class BotSocketThread extends ConsoleSocketThread {
        @Override
        protected void clientMainLoop() throws IOException, ClassNotFoundException {
            sendTextMessage("Привет чатику. Я бот. Понимаю команды: дата, день, месяц, год, время, час, минуты, секунды.");
            super.clientMainLoop();
        }

        @Override
        protected void processIncomingMessage(String message) {
            super.processIncomingMessage(message);

            if (message.contains(":")) {
                String format = "Информация для %s: %s";

                String[] strings = message.split(": ");

                Calendar calendar = Calendar.getInstance();

                switch (strings[1]) {
                    case "дата":
                        sendTextMessage(String.format(format, strings[0]
                                , new SimpleDateFormat("d.MM.YYYY").format(calendar.getTime())));
                        break;
                    case "день":
                        sendTextMessage(String.format(format, strings[0]
                                , new SimpleDateFormat("d").format(calendar.getTime())));
                        break;
                    case "месяц":
                        sendTextMessage(String.format(format, strings[0]
                                , new SimpleDateFormat("MMMM").format(calendar.getTime())));
                        break;
                    case "год":
                        sendTextMessage(String.format(format, strings[0]
                                , new SimpleDateFormat("YYYY").format(calendar.getTime())));
                        break;
                    case "время":
                        sendTextMessage(String.format(format, strings[0]
                                , new SimpleDateFormat("H:mm:ss").format(calendar.getTime())));
                        break;
                    case "час":
                        sendTextMessage(String.format(format, strings[0]
                                , new SimpleDateFormat("H").format(calendar.getTime())));
                        break;
                    case "минуты":
                        sendTextMessage(String.format(format, strings[0]
                                , new SimpleDateFormat("m").format(calendar.getTime())));
                        break;
                    case "секунды":
                        sendTextMessage(String.format(format, strings[0]
                                , new SimpleDateFormat("s").format(calendar.getTime())));
                }
            }
        }
    }
}
