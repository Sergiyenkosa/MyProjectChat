package gui;

import console.Client;
import messages.Message;

import java.io.*;
import java.util.HashMap;
import java.util.Map;

import static messages.Message.FILE_CANCEL;
import static messages.Message.FILE_TRANSFER_ERROR;
import static messages.Message.MessageType.FILE_MESSAGE_RESPONSE;


/**
 * Created by s.sergienko on 19.10.2016.
 */
public class ClientGuiController extends Client {
    private ClientGuiModel model = new ClientGuiModel();
    private ClientGuiView view = new ClientGuiView(this);
    private String userName;

    @Override
    protected SocketThread getSocketThread() {
        return new GuiSocketThread();
    }

    @Override
    public void run() {
        getSocketThread().run();
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
    protected void writeMessage(String message) {
        if (message.toLowerCase().contains("ошибка")) {
            view.errorMessage(message);
        }
        else {
            view.infoMessage(message);
        }
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

    public static void main(String[] args) {
        new ClientGuiController().run();
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

    //temporarily end

    public class GuiSocketThread extends Client.SocketThread {
        @Override
        protected void processIncomingMessage(String message) {
            model.setNewMessage(message);
            view.refreshMessages();
        }

//        @Override
//        protected void processPrivetMessage(String message) {
//            if (message.startsWith("Server:")) {
//                writeMessage(message);
//            }
//            else {
//                processIncomingMessage("Приватное сообщение от " + message);
//            }
//        }

        @Override
        protected void processIncomingFileMessageForAll(Message fileMessageForAll) {
            if(askGetFile(fileMessageForAll.getSenderName(), fileMessageForAll.getData())) {
                File file = getDirectoryFile();

                try (FileOutputStream outputStream = new FileOutputStream(file.getPath() + File.separator + fileMessageForAll.getData())){
                    outputStream.write(fileMessageForAll.getBytes());

                    writeMessage("Файл сохранен");
                }
                catch (IOException e) {
                    writeMessage("Ошибка сохранения файла");
                }
            }
        }

        @Override
        protected void processIncomingFileMessage(Message fileMessage) {
            fileMessage.setType(FILE_MESSAGE_RESPONSE);

            if (askGetFile(fileMessage.getSenderName(), fileMessage.getData())) {
                File file = getDirectoryFile();

                try {
                    FileOutputStream outputStream = new FileOutputStream(file.getPath() + File.separator + fileMessage.getData());

                    int id = ClientGuiController.this.getStreamId();

                    Map<Integer, FileOutputStream> map = new HashMap<>();
                    map.put(id, outputStream);
                    outputStreamsMap.put(fileMessage.getSenderName(), map);

                    fileMessage.setReceiverOutputStreamId(id);

                    writeMessage("Процес получения файла " + fileMessage.getData());
                }
                catch (FileNotFoundException e) {
                    fileMessage.setReceiverOutputStreamId(FILE_TRANSFER_ERROR);

                    writeMessage("Ошибка сохранения файла");
                }
            }
            else {
                fileMessage.setReceiverOutputStreamId(FILE_CANCEL);
            }

            try {
                connection.send(fileMessage);
            } catch (IOException e) {
                clientConnected = false;
            }
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
            view.notifyConnectionStatusChanged(clientConnected);
        }
    }
}
