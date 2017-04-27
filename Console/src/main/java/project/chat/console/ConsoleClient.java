package project.chat.console;

import project.chat.client.Client;
import project.chat.client.SocketThread;
import project.chat.messagehalper.ConsoleHelper;
import project.chat.messagehalper.impl.ConsoleHelperImpl;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;

/**
 * Created by s.sergienko on 06.03.2017.
 */
public class ConsoleClient extends Client {
    final ConsoleHelper consoleHelper = new ConsoleHelperImpl();
    final Object fileRequestLock = new Object();

    @Override
    public void run() {
        SocketThread socketThread = new ConsoleSocketThread(this);
        socketThread.setDaemon(true);
        socketThread.start();

        synchronized (this) {
            try {
                wait();

                if (isClientConnected()) {
                    consoleHelper.writeInfoMessage("Connection is established. " +
                            "For exit type the command 'exit', type 'pm' to send a private message," +
                            " type 'fm' to send a file to one of the users or 'fmfa' for all users.");

                    runConsoleInputMessagesHandler();
                } else {
                    consoleHelper.writeErrorMessage("Error connecting to the server");
                }
            } catch (InterruptedException e) {
                setClientConnected(false);
            }
        }
    }

    @Override
    protected String getServerAddress() {
        consoleHelper.writeInfoMessage("Type the server ip");
        return consoleHelper.readString();
    }

    @Override
    protected int getServerPort() {
        consoleHelper.writeInfoMessage("Type the server port");
        return consoleHelper.readInt();
    }

    @Override
    protected String getUserName() {
        consoleHelper.writeInfoMessage("Type the login");
        return consoleHelper.readString();
    }

    @Override
    protected String getUserPassword() {
        consoleHelper.writeInfoMessage("Type the user password");
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
            String questionMessage = String.format("User %s send file %s for you, download the file? yes / no",
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
            consoleHelper.writeInfoMessage("Type full path name to the folder to save the file");

            File file = new File(consoleHelper.readString());

            if (file.isDirectory()) {
                return file;
            } else {
                consoleHelper.writeErrorMessage("Path error, please try again.");
            }
        }
    }

    @Override
    protected String askForExit() {
        return super.askForExit() + " yes / no";
    }

    private boolean shouldSentTextFromConsole() {
        return true;
    }

    private void runConsoleInputMessagesHandler() throws InterruptedException {
        while (true) {
            if (isClientConnected()) {
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
                consoleHelper.writeErrorMessage("Error connecting to the server is lost.");
                closeAndRemoveAllStreams(true);
                break;
            }
        }
    }

    private void createPrivateMessage (){
        consoleHelper.writeInfoMessage("Type user name");
        String receiverName = consoleHelper.readString();

        consoleHelper.writeInfoMessage("Type private message text ");
        String privateMessage = consoleHelper.readString();

        sendPrivateMessage(privateMessage, receiverName);
    }

    private void createFileMessageForAll() {
        consoleHelper.writeInfoMessage("Type full path name to the file, file must not exceed 10 megabytes");

        File file = new File(consoleHelper.readString());

        try {
            sendFileMessageForAll(file.getName(), new FileInputStream(file));
        } catch (FileNotFoundException e) {
            writeErrorMessage("Error file does not exist");
        }
    }

    private void createFileMessage() {
        consoleHelper.writeInfoMessage("Type the exact name of the receiver");

        String receiverName = consoleHelper.readString();

        consoleHelper.writeInfoMessage("Type full path name to the file");

        File file = new File(consoleHelper.readString());

        try {
            sendFileMessage(receiverName, file.getName(), new FileInputStream(file));
        } catch (FileNotFoundException e) {
            writeErrorMessage("Error file does not exist");
        }
    }

    @Override
    protected void closeAndRemoveAllReceiverStreamsFromInputStreamsMap(String receiverName, boolean showErrorMessage) {
        super.closeAndRemoveAllReceiverStreamsFromInputStreamsMap(receiverName, showErrorMessage);
    }

    @Override
    protected void closeAndRemoveAllSenderStreamsFromOutputStreamsMap(String senderName, boolean showErrorMessage) {
        super.closeAndRemoveAllSenderStreamsFromOutputStreamsMap(senderName, showErrorMessage);
    }

    @Override
    protected void setClientConnected(boolean clientConnected) {
        super.setClientConnected(clientConnected);
    }
}
