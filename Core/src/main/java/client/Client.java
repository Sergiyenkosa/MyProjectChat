package client;

import connection.Connection;
import connection.impl.ConnectionImpl;
import messages.Message;
import messages.MessageFactory;

import java.io.*;
import java.lang.reflect.Field;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import static messages.Message.*;
import static messages.Message.MessageType.*;

/**
 * Created by s.sergienko on 18.10.2016.
 */
public abstract class Client{
    private Connection connection;

    private volatile boolean clientConnected = false;

    private Map<String, Map<Integer, FileInputStream>> inputStreamsMap = new ConcurrentHashMap<>();
    private Map<String, Map<Integer, FileOutputStream>> outputStreamsMap = new ConcurrentHashMap<>();

    private final Object closeAndRemoveFromInputStreamsMapLock = new Object();
    private final Object closeAndRemoveFromOutputStreamMapLock = new Object();

    private AtomicInteger id = new AtomicInteger(1);

    protected abstract void run();

    protected abstract String getServerAddress();

    protected abstract int getServerPort();

    protected abstract String getUserName();

    protected abstract String getUserPassword();

    protected abstract void writeInfoMessage(String infoMessage);

    protected abstract void writeErrorMessage(String errorMessage);

    protected abstract boolean askGetFile (String senderName, String fileName);

    protected abstract File getDirectoryFile ();

    protected Integer getStreamId() {
        return id.getAndIncrement();
    }

    protected boolean isClientConnected() {
        return clientConnected;
    }

    protected void setClientConnected(boolean clientConnected) {
        this.clientConnected = clientConnected;
    }

    protected void sendTextMessage(String textMessage) {
        try {
            connection.send(MessageFactory.getTextMessage(textMessage));
        } catch (IOException e) {
            clientConnected = false;
        }
    }

    protected void sendPrivateMessage(String privateMessage, String receiverName) {
        try {
            connection.send(MessageFactory.getPrivateMessage(privateMessage, receiverName));
        } catch (IOException e) {
            clientConnected = false;
        }
    }

    protected void sendFileMessageForAll(String fileName, FileInputStream fileInputStream) {
        try (BufferedInputStream inputStream = new BufferedInputStream(fileInputStream)){
            int byteSize = inputStream.available();

            if (byteSize > 1024*1024*10) {
                writeErrorMessage("File must not exceed 10mb");
            } else {
                byte[] bytes = new byte[byteSize];
                inputStream.read(bytes);

                try {
                    connection.send(MessageFactory.getFileMessageForAll(fileName, bytes));

                    writeInfoMessage("File sent");
                } catch (IOException e) {
                    clientConnected = false;
                }
            }
        } catch (IOException e) {
            writeErrorMessage("File reading error");
        }
    }

    protected void sendFileMessage(String receiverName, String fileName, FileInputStream fileInputStream) {
        try {
            Integer id = getStreamId();

            synchronized (closeAndRemoveFromInputStreamsMapLock) {
                if (inputStreamsMap.containsKey(receiverName)) {
                    inputStreamsMap.get(receiverName).put(id, fileInputStream);
                } else {
                    Map<Integer, FileInputStream> map = new HashMap<>();
                    map.put(id, fileInputStream);

                    inputStreamsMap.put(receiverName, map);
                }
            }

            connection.send(MessageFactory.getFileMessage(fileName, receiverName, id));
        } catch (IOException e) {
            clientConnected = false;
        }
    }

    protected String askForExit() {
        if (inputStreamsMap.size() > 0 && outputStreamsMap.size() > 0) {
            return "You have unfinished downloading and uploading files. Do you want to interrupt them?";
        } else if (inputStreamsMap.size() > 0) {
            return "You have unfinished uploading files. Do you want to interrupt them?";
        } else if (outputStreamsMap.size() > 0) {
            return "You have unfinished downloading files. Do you want to interrupt them?";
        } else {
            return "Do you really want to exit?";
        }
    }

    protected void closeAndRemoveAllStreams(boolean showErrorMessage) {
        for (Map.Entry<String, Map<Integer, FileInputStream>> entry : inputStreamsMap.entrySet()) {
            closeAndRemoveAllReceiverStreamsFromInputStreamsMap(entry.getKey(), showErrorMessage);
        }

        for (Map.Entry<String, Map<Integer, FileOutputStream>> entry : outputStreamsMap.entrySet()) {
            closeAndRemoveAllSenderStreamsFromOutputStreamsMap(entry.getKey(), showErrorMessage);
        }
    }

    protected void closeAndRemoveStreamFromInputStreamsMap(String receiverName, int fileSenderId) {
        synchronized (closeAndRemoveFromInputStreamsMapLock) {
            if (inputStreamsMap.containsKey(receiverName) &&
                    inputStreamsMap.get(receiverName).containsKey(fileSenderId)) {

                try(FileInputStream inputStream = inputStreamsMap.get(receiverName).get(fileSenderId)) {
                    inputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }

                if (inputStreamsMap.get(receiverName).size() == 1) {
                    inputStreamsMap.remove(receiverName);
                } else {
                    inputStreamsMap.get(receiverName).remove(fileSenderId);
                }
            }
        }
    }

    protected void closeAndRemoveStreamFromOutputStreamMap(
            String senderName, int fileReceiverId, boolean isFileDownloaded) {

        synchronized (closeAndRemoveFromOutputStreamMapLock) {
            if (outputStreamsMap.containsKey(senderName)) {
                File file = new File("");

                try(FileOutputStream outputStream = outputStreamsMap.get(senderName).get(fileReceiverId)) {
                    Field pathField = outputStream.getClass().getDeclaredField("path");
                    pathField.setAccessible(true);
                    file = new File((String) pathField.get(outputStream));

                    outputStream.flush();
                    outputStream.close();
                } catch (IOException | IllegalAccessException | NoSuchFieldException e) {
                    e.printStackTrace();
                }

                if (!isFileDownloaded) {
                    file.delete();
                }

                if (outputStreamsMap.get(senderName).size() == 1) {
                    outputStreamsMap.remove(senderName);
                } else {
                    outputStreamsMap.get(senderName).remove(fileReceiverId);
                }
            }
        }
    }

    protected void closeAndRemoveAllReceiverStreamsFromInputStreamsMap(
            String receiverName, boolean showErrorMessage) {

        synchronized (closeAndRemoveFromInputStreamsMapLock) {
            if (inputStreamsMap.containsKey(receiverName)) {
                for (Map.Entry<Integer, FileInputStream> entry : inputStreamsMap.get(receiverName).entrySet()) {
                    try(FileInputStream inputStream = entry.getValue()) {
                        Field pathField = inputStream.getClass().getDeclaredField("path");
                        pathField.setAccessible(true);
                        File file = new File((String) pathField.get(inputStream));

                        if (showErrorMessage) {
                            writeErrorMessage(String.format("Error sending file: %s to user: %s"
                                    , file.getName(), receiverName));
                        }

                        inputStream.close();
                    } catch (IOException | IllegalAccessException | NoSuchFieldException e) {
                        e.printStackTrace();
                    }
                }
                inputStreamsMap.remove(receiverName);
            }
        }
    }

    protected void closeAndRemoveAllSenderStreamsFromOutputStreamsMap(
            String senderName, boolean showErrorMessage) {

        synchronized (closeAndRemoveFromOutputStreamMapLock) {
            if (outputStreamsMap.containsKey(senderName)) {
                for (Map.Entry<Integer, FileOutputStream> entry : outputStreamsMap.get(senderName).entrySet()) {
                    File file = new File("");

                    try(FileOutputStream outputStream = entry.getValue()) {
                        Field pathField = outputStream.getClass().getDeclaredField("path");
                        pathField.setAccessible(true);
                        file = new File((String) pathField.get(outputStream));

                        if (showErrorMessage) {
                            writeErrorMessage(String.format("Error receiving file %s, from user %s"
                                    , file.getName(), senderName));
                        }

                        outputStream.close();
                    } catch (IOException | IllegalAccessException | NoSuchFieldException e) {
                        e.printStackTrace();
                    }

                    file.delete();
                }
                outputStreamsMap.remove(senderName);
            }
        }
    }

    protected abstract class SocketThread extends Thread {

        protected abstract void processIncomingMessage(String message);

        protected abstract void informAboutAddingNewUser(String userName);

        protected abstract void informAboutDeletingNewUser(String userName);

        protected abstract void notifyConnectionStatusChanged(boolean clientConnected);

        protected void processInfoMessage(String infoMessage) {
            writeInfoMessage(infoMessage);
        }

        protected void processErrorMessage(String errorMessage) {
            writeErrorMessage(errorMessage);
        }

        protected void processIncomingFileMessageForAll(Message fileMessageForAll) {
            if (askGetFile(fileMessageForAll.getSenderName(), fileMessageForAll.getData())) {
                File file = getDirectoryFile();

                try (BufferedOutputStream outputStream = new BufferedOutputStream(
                        new FileOutputStream(file.getPath() + File.separator + fileMessageForAll.getData()))) {

                    outputStream.write(fileMessageForAll.getBytes());
                    outputStream.flush();

                    writeInfoMessage("File saved");
                } catch (IOException e) {
                    writeErrorMessage("Error saving file");
                }
            }
        }

        protected void processIncomingFileMessage(Message fileMessage) {
            if (askGetFile(fileMessage.getSenderName(), fileMessage.getData())) {
                try {
                    synchronized (closeAndRemoveFromOutputStreamMapLock) {
                        FileOutputStream outputStream
                                = new FileOutputStream(getDirectoryFile().getPath() +
                                File.separator + fileMessage.getData());
                        Integer id = getStreamId();
                        String senderName = fileMessage.getSenderName();

                        if (outputStreamsMap.containsKey(senderName)) {
                            outputStreamsMap.get(senderName).put(id, outputStream);
                        } else {
                            Map<Integer, FileOutputStream> map = new HashMap<>();
                            map.put(id, outputStream);
                            outputStreamsMap.put(senderName, map);
                        }

                        writeInfoMessage("File obtaining process" + fileMessage.getData());

                        fileMessage.setType(FILE_MESSAGE_RESPONSE);
                        fileMessage.setReceiverOutputStreamId(id);
                    }
                } catch (FileNotFoundException e) {
                    writeErrorMessage("Error saving file" + fileMessage.getData());

                    fileMessage.setType(FILE_MESSAGE_RESPONSE);
                    fileMessage.setReceiverOutputStreamId(FILE_TRANSFER_ERROR);
                }
            } else {
                fileMessage.setType(FILE_MESSAGE_RESPONSE);
                fileMessage.setReceiverOutputStreamId(FILE_CANCEL);
            }

            try {
                connection.send(fileMessage);
            } catch (IOException e) {
                clientConnected = false;
            }
        }

        protected void processIncomingFileMessageRequest(Message fileMessage) {
            synchronized (closeAndRemoveFromOutputStreamMapLock) {
                if (fileMessage.getSenderInputStreamId() == Message.FILE_TRANSFER_ERROR) {
                    closeAndRemoveStreamFromOutputStreamMap(fileMessage.getSenderName()
                            , fileMessage.getReceiverOutputStreamId(), false);
                } else {
                    if (outputStreamsMap.containsKey(fileMessage.getSenderName()) &&
                            outputStreamsMap.get(fileMessage.getSenderName())
                                    .containsKey(fileMessage.getReceiverOutputStreamId())) {
                        try {
                            BufferedOutputStream outputStream = new BufferedOutputStream(outputStreamsMap.
                                    get(fileMessage.getSenderName())
                                    .get(fileMessage.getReceiverOutputStreamId()));

                            outputStream.write(fileMessage.getBytes());
                            outputStream.flush();

                            if (fileMessage.getSenderInputStreamId() == Message.FILE_IS_UPLOADED) {
                                closeAndRemoveStreamFromOutputStreamMap(fileMessage.getSenderName()
                                        , fileMessage.getReceiverOutputStreamId(), true);

                                writeInfoMessage(String.format("File %s from user %s is uploaded"
                                        , fileMessage.getData(), fileMessage.getSenderName()));

                                fileMessage.setType(FILE_MESSAGE_RESPONSE);
                                fileMessage.setReceiverOutputStreamId(FILE_IS_DOWNLOADED);
                                fileMessage.setBytes(null);
                            } else {

                                fileMessage.setType(FILE_MESSAGE_RESPONSE);
                                fileMessage.setBytes(null);
                            }
                        } catch (IOException e) {
                            writeErrorMessage(String.format("Error writing file %s from user %s"
                                    , fileMessage.getData(), fileMessage.getSenderName()));

                            closeAndRemoveStreamFromOutputStreamMap(fileMessage.getSenderName()
                                    , fileMessage.getReceiverOutputStreamId(), false);

                            fileMessage.setType(FILE_MESSAGE_RESPONSE);
                            fileMessage.setReceiverOutputStreamId(FILE_TRANSFER_ERROR);
                            fileMessage.setBytes(null);
                        }

                        try {
                            connection.send(fileMessage);
                        } catch (IOException e) {
                            clientConnected = false;
                        }
                    }
                }
            }
        }

        protected void processIncomingFileMessageResponse(Message fileMessage) {
            synchronized (closeAndRemoveFromInputStreamsMapLock) {
                if (inputStreamsMap.containsKey(fileMessage.getReceiverName()) &&
                        inputStreamsMap.get(fileMessage.getReceiverName())
                                .containsKey(fileMessage.getSenderInputStreamId())) {
                    if (fileMessage.getReceiverOutputStreamId() < 0) {
                        closeAndRemoveStreamFromInputStreamsMap(fileMessage.getReceiverName()
                                , fileMessage.getSenderInputStreamId());
                    } else {
                        try {
                            InputStream inputStream = inputStreamsMap.get(fileMessage.getReceiverName())
                                    .get(fileMessage.getSenderInputStreamId());

                            if (inputStream.available() > 1024 * 6) {
                                byte[] bytes = new byte[1024 * 6];
                                inputStream.read(bytes, 0, bytes.length);

                                fileMessage.setType(FILE_MESSAGE_REQUEST);
                                fileMessage.setBytes(bytes);
                            } else {
                                byte[] bytes = new byte[inputStream.available()];
                                inputStream.read(bytes, 0, bytes.length);

                                fileMessage.setType(FILE_MESSAGE_REQUEST);
                                fileMessage.setSenderInputStreamId(FILE_IS_UPLOADED);
                                fileMessage.setBytes(bytes);

                                closeAndRemoveStreamFromInputStreamsMap(fileMessage.getReceiverName()
                                        , fileMessage.getSenderInputStreamId());
                            }
                        } catch (IOException e) {
                            writeErrorMessage(String.format("Error reading file %s, for user %s"
                                    , fileMessage.getData(), fileMessage.getReceiverName()));

                            closeAndRemoveStreamFromInputStreamsMap(fileMessage.getReceiverName()
                                    , fileMessage.getSenderInputStreamId());

                            fileMessage.setType(FILE_MESSAGE_REQUEST);
                            fileMessage.setSenderInputStreamId(FILE_TRANSFER_ERROR);
                        }

                        try {
                            connection.send(fileMessage);
                        } catch (IOException e) {
                            clientConnected = false;
                        }
                    }
                }
            }
        }

        protected void clientHandshake() throws IOException, ClassNotFoundException {
            while (!isInterrupted()) {
                Message message = connection.receive();

                if (message.getType() == MessageType.CREDENTIALS_REQUEST) {

                    message.setType(USER_CREDENTIALS);
                    message.setData(getUserPassword());
                    message.setSenderName(getUserName());
                    connection.send(message);
                } else if (message.getType() == MessageType.USER_ACCEPTED) {
                    notifyConnectionStatusChanged(true);
                    break;
                } else if (message.getType() == MessageType.ERROR_MESSAGE) {
                    processErrorMessage(message.getData());
                } else {
                    throw new IOException("Unexpected MessageType");
                }
            }
        }

        protected void clientMainLoop() throws IOException, ClassNotFoundException {
            while (clientConnected) {
                Message message = connection.receive();
                MessageType messageType = message.getType();

                if (messageType == MessageType.FILE_MESSAGE_REQUEST) {
                    processIncomingFileMessageRequest(message);
                } else if (messageType == MessageType.FILE_MESSAGE_RESPONSE) {
                    processIncomingFileMessageResponse(message);
                } else if (messageType == MessageType.TEXT_MESSAGE) {
                    processIncomingMessage(message.getData());
                } else if (messageType == MessageType.INFO_MESSAGE) {
                    processInfoMessage(message.getData());
                }else if (messageType == MessageType.ERROR_MESSAGE) {
                    processErrorMessage(message.getData());
                } else if (messageType == MessageType.USER_ADDED) {
                    informAboutAddingNewUser(message.getData());
                } else if (messageType == MessageType.USER_REMOVED) {
                    informAboutDeletingNewUser(message.getData());
                } else if (messageType == MessageType.FILE_MESSAGE) {
                    processIncomingFileMessage(message);
                } else if (messageType == MessageType.FILE_MESSAGE_FOR_ALL) {
                    processIncomingFileMessageForAll(message);
                } else {
                    clientConnected = false;
                    throw new RuntimeException("Unexpected MessageType: " + message.getType());
                }
            }
        }

        @Override
        public void run() {
            String address = getServerAddress();
            int port = getServerPort();

            try {
                Socket socket = new Socket(address, port);

                connection = new ConnectionImpl(socket);

                clientHandshake();

                clientMainLoop();
            } catch (IOException | ClassNotFoundException e) {
                closeAndRemoveAllStreams(true);
                notifyConnectionStatusChanged(false);
            }
        }
    }
}