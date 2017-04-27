package project.chat.console;

import project.chat.client.SocketThread;
import project.chat.messages.Message;

/**
 * Created by s.sergienko on 24.04.2017.
 */
public class ConsoleSocketThread  extends SocketThread {
    private final ConsoleClient consoleClient;

    ConsoleSocketThread(ConsoleClient consoleClient) {
        super(consoleClient);
        this.consoleClient = consoleClient;
    }

    @Override
    protected void processIncomingMessage(String message) {
        consoleClient.consoleHelper.writeTextMessage(message);
    }

    @Override
    protected void processIncomingFileMessageForAll(Message fileMessageForAll) {
        synchronized (consoleClient.fileRequestLock) {
            super.processIncomingFileMessageForAll(fileMessageForAll);
        }
    }

    @Override
    protected void processIncomingFileMessage(Message fileMessage) {
        synchronized (consoleClient.fileRequestLock) {
            super.processIncomingFileMessage(fileMessage);
        }
    }

    @Override
    protected void informAboutAddingNewUser(String userName) {
        consoleClient.writeInfoMessage("User " + userName + " joined the chat");
    }

    @Override
    protected void informAboutDeletingNewUser(String userName) {
        consoleClient.closeAndRemoveAllReceiverStreamsFromInputStreamsMap(userName, true);
        consoleClient.closeAndRemoveAllSenderStreamsFromOutputStreamsMap(userName, true);

        consoleClient.writeInfoMessage("User " + userName + " has left the chat");
    }

    @Override
    protected void notifyConnectionStatusChanged(boolean clientConnected) {
        consoleClient.setClientConnected(clientConnected);

        synchronized (consoleClient) {
            consoleClient.notify();
        }
    }
}
