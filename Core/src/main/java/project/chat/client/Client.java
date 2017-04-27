package project.chat.client;

import org.apache.log4j.Logger;
import project.chat.connection.Connection;
import project.chat.messages.MessageFactory;

import java.io.*;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by s.sergienko on 18.10.2016.
 */
public abstract class Client{
    private static final Logger log = Logger.getLogger(Client.class);

    private AtomicInteger id = new AtomicInteger(1);

    protected Connection connection;

    protected volatile boolean clientConnected = false;

    protected Map<String, Map<Integer, FileInputStream>> inputStreamsMap = new ConcurrentHashMap<>();
    protected Map<String, Map<Integer, FileOutputStream>> outputStreamsMap = new ConcurrentHashMap<>();

    protected final Object closeAndRemoveFromInputStreamsMapLock = new Object();
    protected final Object closeAndRemoveFromOutputStreamMapLock = new Object();

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
                    log.error(e.getMessage());
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
                    log.error(e.getMessage());
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
                        log.error(e.getMessage());
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
                        log.error(e.getMessage());
                    }

                    file.delete();
                }
                outputStreamsMap.remove(senderName);
            }
        }
    }
}