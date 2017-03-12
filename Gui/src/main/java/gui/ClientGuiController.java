package gui;

import client.Client;

import java.io.*;


/**
 * Created by s.sergienko on 19.10.2016.
 */
public class ClientGuiController extends Client {
    private ClientGuiModel model = new ClientGuiModel();
    private ClientGuiView view = new ClientGuiView(this);
    private String userName;

    public static void main(String[] args) {
        new ClientGuiController().run();
    }

    @Override
    protected SocketThread getSocketThread() {
        return new GuiSocketThread();
    }

    @Override
    public void run() {
        SocketThread socketThread = getSocketThread();
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
        return view.getUserName();
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

    public ClientGuiModel getModel() {
        return model;
    }

    //temporarily start

    @Override
    protected void sendPrivateMessage(String privateMessage, String receiverName) {
        super.sendPrivateMessage(privateMessage, receiverName);
    }

    @Override
    protected void sendFileMessageForAll(File file) {
        super.sendFileMessageForAll(file);
    }

    @Override
    protected void sendFileMessage(String receiverName, File file) {
        super.sendFileMessage(receiverName, file);
    }

    @Override
    protected void closeAndRemoveStreamFromInputStreamsMap(String receiverName, int fileSenderId) {
        super.closeAndRemoveStreamFromInputStreamsMap(receiverName, fileSenderId);
    }

    @Override
    protected void closeAndRemoveStreamFromOutputStreamMap(String senderName, int fileReceiverId, boolean isFileDownloaded) {
        super.closeAndRemoveStreamFromOutputStreamMap(senderName, fileReceiverId, isFileDownloaded);
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
    protected void closeAndRemoveAllStreams(boolean showErrorMessage) {
        super.closeAndRemoveAllStreams(showErrorMessage);
    }

    @Override
    protected void sendTextMessage(String textMessage) {
        super.sendTextMessage(textMessage);
    }

    @Override
    protected String askForExit() {
        return super.askForExit();
    }

    //temporarily end

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
            if (ClientGuiController.this.userName != null) {
                if (!ClientGuiController.this.userName.equals(userName)) {
                    processIncomingMessage("Участник с именем " + userName + " присоединился к чату");
                }
            }
        }

        @Override
        protected void informAboutDeletingNewUser(String userName) {
            closeAndRemoveAllReceiverStreamsFromInputStreamsMap(userName, true);
            closeAndRemoveAllSenderStreamsFromOutputStreamsMap(userName,  true);

            model.deleteUser(userName);
            view.refreshUsers();
            if (ClientGuiController.this.userName != null) {
                if (!ClientGuiController.this.userName.equals(userName)) {
                    processIncomingMessage("Участник с именем " + userName + " покинул чат");
                }
            }
        }

        @Override
        protected void notifyConnectionStatusChanged(boolean clientConnected) {
            ClientGuiController.this.clientConnected = clientConnected;

            view.notifyConnectionStatusChanged(clientConnected);
        }
    }
}
