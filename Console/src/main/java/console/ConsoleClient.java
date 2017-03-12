package console;

import client.Client;
import messagehalper.ConsoleHelper;
import messagehalper.impl.ConsoleHelperImpl;
import messages.Message;

import java.io.File;

/**
 * Created by s.sergienko on 06.03.2017.
 */
public class ConsoleClient extends Client {
    private ConsoleHelper consoleHelper = new ConsoleHelperImpl();
    private final Object fileRequestLock = new Object();

    public void setConsoleHelper(ConsoleHelper consoleHelper) {
        this.consoleHelper = consoleHelper;
    }

    public static void main(String[] args) {
        new ConsoleClient().run();
    }

    @Override
    protected SocketThread getSocketThread() {
        return new ConsoleSocketThread();
    }

    @Override
    public void run() {
        SocketThread socketThread = getSocketThread();
        socketThread.setDaemon(true);
        socketThread.start();

        synchronized (this) {
            try {
                wait();

                if (clientConnected) {
                    consoleHelper.writeInfoMessage("Соединение установлено. Для выхода наберите команду 'exit'," +
                            " наберите 'pm' для отправки приватного сообщения," +
                            " наберите fm для отправки файла одному из пользывателей," +
                            " или fmfa для всех пользователей.");

                    runConsoleInputMessagesHandler();
                } else {
                    consoleHelper.writeErrorMessage("Произошла ошибка во время работы клиента.");
                }
            } catch (InterruptedException e) {
                clientConnected = false;
            }
        }
    }

    @Override
    protected String getServerAddress() {
        consoleHelper.writeInfoMessage("Введите адрес сервера");
        return consoleHelper.readString();
    }

    @Override
    protected int getServerPort() {
        return consoleHelper.readInt();
    }

    @Override
    protected String getUserName() {
        consoleHelper.writeInfoMessage("Введите имя пользователя");
        return consoleHelper.readString();
    }

    @Override
    protected String getUserPassword() {
        consoleHelper.writeInfoMessage("Введите пароль");
        return consoleHelper.readString();
    }

    @Override
    protected void writeInfoMessage(String infoMessage) {
        consoleHelper.writeInfoMessage(infoMessage);
    }

    @Override
    protected void writeErrorMessage(String errorMessage) {
        consoleHelper.writeErrorMessage(errorMessage);
    }

    @Override
    protected boolean askGetFile (String senderName, String fileName) {
        while (true) {
            String questionMessage = String.format("Пользователь %s отправил вам файл %s," +
                            " для получения введите yes для отказа введите no",
                    senderName, fileName);
            consoleHelper.writeInfoMessage(questionMessage);

            String answer = consoleHelper.readString();

            switch (answer) {
                case "yes":
                    return true;
                case "no":
                    return false;
            }
        }
    }

    @Override
    protected File getDirectoryFile () {
        while (true) {
            consoleHelper.writeInfoMessage("Введите полный путь к папке для сохранения файла");

            File file = new File(consoleHelper.readString());

            if (file.isDirectory()) {
                return file;
            } else {
                consoleHelper.writeErrorMessage("Вы ввели неправельный путь, повторите еще раз!");
            }
        }
    }

    protected boolean shouldSentTextFromConsole() {
        return true;
    }

    private void runConsoleInputMessagesHandler() throws InterruptedException {
        while (true) {
            if (clientConnected) {
                synchronized (fileRequestLock) {
                    String s = consoleHelper.readString();
                    if (s.equals("exit")) {
                        consoleHelper.writeInfoMessage(askForExit());

                        String answer = consoleHelper.readString();
                        if (answer.equals("yes")) {
                            closeAndRemoveAllStreams(false);

                            break;
                        }
                    } else if (s.equals("pm")) {
                        createPrivateMessage();
                    } else if (s.equals("fmfa")) {
                        createFileMessageForAll();
                    } else if (s.equals("fm")) {
                        createFileMessage();
                    } else if (shouldSentTextFromConsole()) {
                        sendTextMessage(s);
                    }

                    fileRequestLock.wait(1);
                }
            } else {
                consoleHelper.writeErrorMessage("Произошла ошибка во время работы клиента соединение с сервером утеряно.");
                closeAndRemoveAllStreams(true);
                break;
            }
        }
    }

    private void createPrivateMessage (){
        consoleHelper.writeInfoMessage("Введите точное имя получателя");
        String receiverName = consoleHelper.readString();

        consoleHelper.writeInfoMessage("Наберите текст для приватного сообщения");
        String privateMessage = consoleHelper.readString();

        sendPrivateMessage(privateMessage, receiverName);
    }

    private void createFileMessageForAll() {
        consoleHelper.writeInfoMessage("Введите полный путь к файлу, файл не должен превышать 10 мегобайт");

        sendFileMessageForAll(new File(consoleHelper.readString()));
    }

    private void createFileMessage() {
        consoleHelper.writeInfoMessage("Введите точное имя получателя");

        String receiverName = consoleHelper.readString();

        consoleHelper.writeInfoMessage("Введите полный путь к файлу");

        File file = new File(consoleHelper.readString());

        if (file.isFile()) {
            sendFileMessage(receiverName, file);
        } else {
            consoleHelper.writeErrorMessage("Такого файла не существует");
        }
    }

    public class ConsoleSocketThread extends Client.SocketThread {

        @Override
        protected void processIncomingMessage(String message) {
            consoleHelper.writeTextMessage(message);
        }

        @Override
        protected void processIncomingFileMessageForAll(Message fileMessageForAll) {
            synchronized (fileRequestLock) {
                super.processIncomingFileMessageForAll(fileMessageForAll);
            }
        }

        @Override
        protected void processIncomingFileMessage(Message fileMessage) {
            synchronized (fileRequestLock) {
                super.processIncomingFileMessage(fileMessage);
            }
        }

        @Override
        protected void informAboutAddingNewUser(String userName) {
            writeInfoMessage("Участник с именем " + userName + " присоединился к чату");
        }

        @Override
        protected void informAboutDeletingNewUser(String userName) {
            closeAndRemoveAllReceiverStreamsFromInputStreamsMap(userName, true);
            closeAndRemoveAllSenderStreamsFromOutputStreamsMap(userName, true);

            writeInfoMessage("Участник с именем " + userName + " покинул чат");
        }

        @Override
        protected void notifyConnectionStatusChanged(boolean clientConnected) {
            ConsoleClient.this.clientConnected = clientConnected;

            synchronized (ConsoleClient.this) {
                ConsoleClient.this.notify();
            }
        }
    }
}
