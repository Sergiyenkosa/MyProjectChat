package project.chat.gui;

import project.chat.client.SocketThread;

/**
 * Created by s.sergienko on 24.04.2017.
 */
public class GuiSocketThread  extends SocketThread {
    private final GuiControllerClient controllerClient;

    GuiSocketThread(GuiControllerClient controllerClient) {
        super(controllerClient);
        this.controllerClient = controllerClient;
    }

    @Override
    protected void processIncomingMessage(String message) {
        controllerClient.model.setNewMessage(message);
        controllerClient.view.refreshMessages();
    }

    @Override
    protected void informAboutAddingNewUser(String userName) {
        controllerClient.model.addUser(userName);
        controllerClient.view.refreshUsers();

        if (!controllerClient.userName.equals(userName)) {
            processIncomingMessage("User " + userName + " joined the chat");
        }
    }

    @Override
    protected void informAboutDeletingNewUser(String userName) {
        controllerClient.closeAndRemoveAllReceiverStreamsFromInputStreamsMap(userName, true);
        controllerClient.closeAndRemoveAllSenderStreamsFromOutputStreamsMap(userName,  true);

        controllerClient.model.deleteUser(userName);
        controllerClient.view.refreshUsers();

        if (!controllerClient.userName.equals(userName)) {
            processIncomingMessage("User " + userName + " has left the chat");
        }
    }

    @Override
    protected void notifyConnectionStatusChanged(boolean clientConnected) {
        controllerClient.setClientConnected(clientConnected);

        controllerClient.view.notifyConnectionStatusChanged(clientConnected);
    }
}
