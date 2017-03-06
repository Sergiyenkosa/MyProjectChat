package console;

import messages.Message;

import java.io.File;

/**
 * Created by s.sergienko on 06.03.2017.
 */
public class ConsoleClient extends Client{
    private final Object fileRequestLock = new Object();

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
                    messageHelper.writeInfoMessage("Соединение установлено. Для выхода наберите команду 'exit'," +
                            " наберите 'pm' для отправки приватного сообщения," +
                            " наберите fm для отправки файла одному из пользывателей," +
                            " или fmfa для всех пользователей.");

                    runConsoleInputMessagesHandler();
                } else {
                    messageHelper.writeErrorMessage("Произошла ошибка во время работы клиента.");
                }
            } catch (InterruptedException e) {
                clientConnected = false;
            }
        }
    }

    @Override
    protected String getServerAddress() {
        messageHelper.writeInfoMessage("Введите адрес сервера");
        return messageHelper.readString();
    }

    @Override
    protected int getServerPort() {
        return messageHelper.readInt();
    }

    @Override
    protected String getUserName() {
        messageHelper.writeInfoMessage("Введите имя пользователя");
        return messageHelper.readString();
    }

    @Override
    protected boolean askGetFile (String senderName, String fileName) {
        while (true) {
            String questionMessage = String.format("Пользователь %s отправил вам файл %s," +
                            " для получения введите yes для отказа введите no",
                    senderName, fileName);
            messageHelper.writeInfoMessage(questionMessage);

            String answer = messageHelper.readString();

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
            messageHelper.writeInfoMessage("Введите полный путь к папке для сохранения файла");

            File file = new File(messageHelper.readString());

            if (file.isDirectory()) {
                return file;
            } else {
                messageHelper.writeErrorMessage("Вы ввели неправельный путь, повторите еще раз!");
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
                    String s = messageHelper.readString();
                    if (s.equals("exit")) {
                        messageHelper.writeInfoMessage(askForExit());

                        String answer = messageHelper.readString();
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
                messageHelper.writeErrorMessage("Произошла ошибка во время работы клиента соединение с сервером утеряно.");
                closeAndRemoveAllStreams(true);
                break;
            }
        }
    }

    private void createPrivateMessage (){
        messageHelper.writeInfoMessage("Введите точное имя получателя");
        String receiverName = messageHelper.readString();

        messageHelper.writeInfoMessage("Наберите текст для приватного сообщения");
        String privateMessage = messageHelper.readString();

        sendPrivateMessage(privateMessage, receiverName);
    }

    private void createFileMessageForAll() {
        messageHelper.writeInfoMessage("Введите полный путь к файлу, файл не должен превышать 10 мегобайт");

        sendFileMessageForAll(new File(messageHelper.readString()));
    }

    private void createFileMessage() {
        messageHelper.writeInfoMessage("Введите точное имя получателя");

        String receiverName = messageHelper.readString();

        messageHelper.writeInfoMessage("Введите полный путь к файлу");

        File file = new File(messageHelper.readString());

        if (file.isFile()) {
            sendFileMessage(receiverName, file);
        } else {
            messageHelper.writeErrorMessage("Такого файла не существует");
        }
    }

    public class ConsoleSocketThread extends Client.SocketThread {
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
    }
}
