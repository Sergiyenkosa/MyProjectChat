package gui;

import client.Client;

import java.io.*;


/**
 * Created by s.sergienko on 19.10.2016.
 */
public class GuiControllerClient extends Client {
    private GuiModelClient model = new GuiModelClient();
    private GuiViewClient view = new GuiViewClient(this);
    private String userName;

    public static void main(String[] args) {
        new GuiControllerClient().run();
    }

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
                processIncomingMessage("Участник с именем " + userName + " присоединился к чату");
            }
        }

        @Override
        protected void informAboutDeletingNewUser(String userName) {
            closeAndRemoveAllReceiverStreamsFromInputStreamsMap(userName, true);
            closeAndRemoveAllSenderStreamsFromOutputStreamsMap(userName,  true);

            model.deleteUser(userName);
            view.refreshUsers();

            if (!GuiControllerClient.this.userName.equals(userName)) {
                processIncomingMessage("Участник с именем " + userName + " покинул чат");
            }
        }

        @Override
        protected void notifyConnectionStatusChanged(boolean clientConnected) {
            GuiControllerClient.this.setClientConnected(clientConnected);

            view.notifyConnectionStatusChanged(clientConnected);
        }
    }
}
