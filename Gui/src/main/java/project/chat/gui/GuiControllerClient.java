package project.chat.gui;

import project.chat.client.Client;
import project.chat.client.SocketThread;

import java.io.File;
import java.io.FileInputStream;


/**
 * Created by s.sergienko on 19.10.2016.
 */
public class GuiControllerClient extends Client {
    GuiModelClient model = new GuiModelClient();
    GuiViewClient view = new GuiViewClient(this);
    String userName;

    @Override
    public void run() {
        SocketThread socketThread = new GuiSocketThread(this);
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

    @Override
    protected void setClientConnected(boolean clientConnected) {
        super.setClientConnected(clientConnected);
    }

    @Override
    protected void closeAndRemoveAllReceiverStreamsFromInputStreamsMap(String receiverName, boolean showErrorMessage) {
        super.closeAndRemoveAllReceiverStreamsFromInputStreamsMap(receiverName, showErrorMessage);
    }

    @Override
    protected void closeAndRemoveAllSenderStreamsFromOutputStreamsMap(String senderName, boolean showErrorMessage) {
        super.closeAndRemoveAllSenderStreamsFromOutputStreamsMap(senderName, showErrorMessage);
    }

    public String getName() {
        return userName;
    }

    public GuiModelClient getModel() {
        return model;
    }
}
