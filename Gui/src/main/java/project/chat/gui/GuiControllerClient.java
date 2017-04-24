package project.chat.gui;

import project.chat.client.Client;

import java.io.*;


/**
 * Created by s.sergienko on 19.10.2016.
 */
public class GuiControllerClient extends Client {
    private GuiModelClient model = new GuiModelClient();
    private GuiViewClient view = new GuiViewClient(this);
    private String userName;

    @Override
    public void run() {
        SocketThread socketThread = new GuiSocketThread();
        socketThread.run();
    }

    @Override
    protected String getServerAddress() {
        return view.getServerAddress();
    }

    @Override
    protected int getServerPort() {
        return view.getServerPort();
    }

    @Override
    protected String getUserName() {
        userName = view.getUserName();
        return userName;
    }

    @Override
    protected String getUserPassword() {
        return view.getUserPassword();
    }

    @Override
    protected void writeInfoMessage(String infoMessage) {
        view.infoMessage(infoMessage);
    }

    @Override
    protected void writeErrorMessage(String errorMessage) {
        view.errorMessage(errorMessage);
    }

    @Override
    protected boolean askGetFile(String senderName, String fileName) {
        return view.askGetFile(senderName, fileName);
    }

    @Override
    protected File getDirectoryFile() {
        return view.getDirectoryFile();
    }

    @Override
    protected void sendTextMessage(String textMessage) {
        super.sendTextMessage(textMessage);
    }

    @Override
    protected void sendPrivateMessage(String privateMessage, String receiverName) {
        super.sendPrivateMessage(privateMessage, receiverName);
    }

    @Override
    protected void sendFileMessageForAll(String fileName, FileInputStream fileInputStream) {
        super.sendFileMessageForAll(fileName, fileInputStream);
    }

    @Override
    protected void sendFileMessage(String receiverName, String fileName, FileInputStream fileInputStream) {
        super.sendFileMessage(receiverName, fileName, fileInputStream);
    }

    @Override
    protected String askForExit() {
        return super.askForExit();
    }

    @Override
    protected void closeAndRemoveAllStreams(boolean showErrorMessage) {
        super.closeAndRemoveAllStreams(showErrorMessage);
    }

    public String getName() {
        return userName;
    }

    public GuiModelClient getModel() {
        return model;
    }

    public class GuiSocketThread extends Client.SocketThread {
        @Override
        protected void processIncomingMessage(String message) {
            model.setNewMessage(message);
            view.refreshMessages();
        }

        @Override
        protected void informAboutAddingNewUser(String userName) {
            model.addUser(userName);
            view.refreshUsers();

            if (!GuiControllerClient.this.userName.equals(userName)) {
                processIncomingMessage("User " + userName + " joined the chat");
            }
        }

        @Override
        protected void informAboutDeletingNewUser(String userName) {
            closeAndRemoveAllReceiverStreamsFromInputStreamsMap(userName, true);
            closeAndRemoveAllSenderStreamsFromOutputStreamsMap(userName,  true);

            model.deleteUser(userName);
            view.refreshUsers();

            if (!GuiControllerClient.this.userName.equals(userName)) {
                processIncomingMessage("User " + userName + " has left the chat");
            }
        }

        @Override
        protected void notifyConnectionStatusChanged(boolean clientConnected) {
            GuiControllerClient.this.setClientConnected(clientConnected);

            view.notifyConnectionStatusChanged(clientConnected);
        }
    }
}
