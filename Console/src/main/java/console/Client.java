package console;

import connection.Connection;
import connection.impl.ConnectionImpl;
import messagehandlers.TextMessageHelper;
import messagehandlers.impl.TextMessageConsoleHelper;
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

/**
 * Created by s.sergienko on 18.10.2016.
 */
public abstract class Client{
    private Connection connection;
    protected volatile boolean clientConnected = false;
    private Map<String, Map<Integer, FileInputStream>> inputStreamsMap = new ConcurrentHashMap<>();
    private Map<String, Map<Integer, FileOutputStream>> outputStreamsMap = new ConcurrentHashMap<>();
    public TextMessageHelper messageHelper = new TextMessageConsoleHelper();

    private final Object closeAndRemoveFromInputStreamsMapLock = new Object();
    private final Object closeAndRemoveFromOutputStreamMapLock = new Object();

    private AtomicInteger id = new AtomicInteger(1);

    protected abstract void run();

    protected abstract SocketThread getSocketThread();

    protected abstract String getServerAddress();

    protected abstract int getServerPort();

    protected abstract String getUserName();

    protected abstract boolean askGetFile (String senderName, String fileName);

    protected abstract File getDirectoryFile ();

    private Integer getStreamId() {
        return id.getAndIncrement();
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

    protected void sendFileMessageForAll(File file) {
        if (!file.isFile()) {
            messageHelper.writeErrorMessage("Такого файла не существует");
        } else if (file.length() > 1024*1024*10) {
            messageHelper.writeErrorMessage("Файл не должен превышать 10 мегобайт");
        } else {
            try (BufferedInputStream inputStream = new BufferedInputStream(new FileInputStream(file))){
                byte[] bytes = new byte[inputStream.available()];
                inputStream.read(bytes);

                connection.send(MessageFactory.getFileMessageForAll(file.getName(), bytes));

                messageHelper.writeInfoMessage("Файл отправлен");
            } catch (FileNotFoundException e) {
                messageHelper.writeErrorMessage("Ошибка чтения файла");
            } catch (IOException e) {
                clientConnected = false;
            }
        }
    }

    protected void sendFileMessage(String receiverName, File file) {
        try {
            FileInputStream inputStream = new FileInputStream(file);

            Integer id = getStreamId();

            synchronized (closeAndRemoveFromInputStreamsMapLock) {
                if (inputStreamsMap.containsKey(receiverName)) {
                    inputStreamsMap.get(receiverName).put(id, inputStream);
                } else {
                    Map<Integer, FileInputStream> map = new HashMap<>();
                    map.put(id, inputStream);

                    inputStreamsMap.put(receiverName, map);
                }
            }

            connection.send(MessageFactory.getFileMessage(file.getName(), receiverName, id));
        } catch (FileNotFoundException e) {
            messageHelper.writeErrorMessage("Ошибка чтения файла");
        } catch (IOException e) {
            clientConnected = false;
        }
    }

    protected String askForExit() {
        if (inputStreamsMap.size() > 0 && outputStreamsMap.size() > 0) {
            return "У вас есть незавершенный прием и передача файлов, вы хотите прервать их? yes / no";
        } else if (inputStreamsMap.size() > 0) {
            return "У вас есть незавершенная передача файлов, вы хотите прервать ее? yes / no";
        } else if (outputStreamsMap.size() > 0) {
            return "У вас есть незавершенный прием файлов, вы хотите прервать его? yes / no";
        } else {
            return "Вы действительно хотите выйти из программы? yes / no";
        }
    }

    protected void closeAndRemoveStreamFromInputStreamsMap(String receiverName, int fileSenderId) {
        synchronized (closeAndRemoveFromInputStreamsMapLock) {
            if (inputStreamsMap.containsKey(receiverName) && inputStreamsMap.get(receiverName).containsKey(fileSenderId)) {
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

    protected void closeAndRemoveStreamFromOutputStreamMap(String senderName, int fileReceiverId, boolean isFileDownloaded) {
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

    protected void closeAndRemoveAllReceiverStreamsFromInputStreamsMap(String receiverName, boolean showErrorMessage) {
        synchronized (closeAndRemoveFromInputStreamsMapLock) {
            if (inputStreamsMap.containsKey(receiverName)) {
                for (Map.Entry<Integer, FileInputStream> entry : inputStreamsMap.get(receiverName).entrySet()) {
                    try(FileInputStream inputStream = entry.getValue()) {
                        Field pathField = inputStream.getClass().getDeclaredField("path");
                        pathField.setAccessible(true);
                        File file = new File((String) pathField.get(inputStream));

                        if (showErrorMessage) {
                            messageHelper.writeErrorMessage(String.format("Ошибка передачи файла: %s учаснику: %s"
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

    protected void closeAndRemoveAllSenderStreamsFromOutputStreamsMap(String senderName, boolean showErrorMessage) {
        synchronized (closeAndRemoveFromOutputStreamMapLock) {
            if (outputStreamsMap.containsKey(senderName)) {
                for (Map.Entry<Integer, FileOutputStream> entry : outputStreamsMap.get(senderName).entrySet()) {
                    File file = new File("");

                    try(FileOutputStream outputStream = entry.getValue()) {
                        Field pathField = outputStream.getClass().getDeclaredField("path");
                        pathField.setAccessible(true);
                        file = new File((String) pathField.get(outputStream));

                        if (showErrorMessage) {
                            messageHelper.writeErrorMessage(String.format("Ошибка приема файла: %s от учасника: %s"
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

    protected void closeAndRemoveAllStreams(boolean showErrorMessage) {
        for (Map.Entry<String, Map<Integer, FileInputStream>> entry : inputStreamsMap.entrySet()) {
            closeAndRemoveAllReceiverStreamsFromInputStreamsMap(entry.getKey(), showErrorMessage);
        }

        for (Map.Entry<String, Map<Integer, FileOutputStream>> entry : outputStreamsMap.entrySet()) {
            closeAndRemoveAllSenderStreamsFromOutputStreamsMap(entry.getKey(), showErrorMessage);
        }
    }

    public abstract class SocketThread extends Thread {
        protected void processIncomingMessage(String message) {
            messageHelper.writeTextMessage(message);
        }

        protected void processErrorMessage(String errorMessage) {
            messageHelper.writeErrorMessage(errorMessage);
        }

        protected void processIncomingFileMessageForAll(Message fileMessageForAll) {
            if (askGetFile(fileMessageForAll.getSenderName(), fileMessageForAll.getData())) {
                File file = getDirectoryFile();

                try (FileOutputStream outputStream = new FileOutputStream(file.getPath() + File.separator + fileMessageForAll.getData())) {
                    outputStream.write(fileMessageForAll.getBytes());

                    messageHelper.writeInfoMessage("Файл сохранен");
                } catch (IOException e) {
                    messageHelper.writeErrorMessage("Ощибка сохранения файла");
                }
            }
        }

        protected void processIncomingFileMessage(Message fileMessage) {
            if (askGetFile(fileMessage.getSenderName(), fileMessage.getData())) {
                File file = getDirectoryFile();

                try {
                    FileOutputStream outputStream
                            = new FileOutputStream(file.getPath() + File.separator + fileMessage.getData());
                    Integer id = getStreamId();
                    String senderName = fileMessage.getSenderName();

                    synchronized (closeAndRemoveFromOutputStreamMapLock) {
                        if (outputStreamsMap.containsKey(senderName)) {
                            outputStreamsMap.get(senderName).put(id, outputStream);
                        } else {
                            Map<Integer, FileOutputStream> map = new HashMap<>();
                            map.put(id, outputStream);
                            outputStreamsMap.put(senderName, map);
                        }
                    }

                    connection.send(MessageFactory.getFileMessageAnswer(fileMessage, id));

                    messageHelper.writeInfoMessage("Процес получения файла " + fileMessage.getData());
                } catch (FileNotFoundException e) {
                    try {
                        connection.send(MessageFactory.getFileTransferErrorResponseMessageFromClient(fileMessage));
                    } catch (IOException e1) {
                        clientConnected = false;
                    }
                    messageHelper.writeErrorMessage("Ощибка сохранения файла");
                } catch (IOException e) {
                    clientConnected = false;
                }
            } else {
                try {
                    connection.send(MessageFactory.getFileCancelResponseMessage(fileMessage));
                } catch (IOException e) {
                    clientConnected = false;
                }
            }
        }

        protected void processFileMessageRequest(Message requestMessage) {
            if (requestMessage.getSenderInputStreamId() == Message.FILE_TRANSFER_ERROR) {
                closeAndRemoveStreamFromOutputStreamMap(requestMessage.getSenderName()
                        , requestMessage.getReceiverOutputStreamId(), false);
            } else {
                try {
                    synchronized (closeAndRemoveFromOutputStreamMapLock) {
                        if (outputStreamsMap.containsKey(requestMessage.getSenderName()) &&
                                outputStreamsMap.get(requestMessage.getSenderName())
                                        .containsKey(requestMessage.getReceiverOutputStreamId())) {
                            outputStreamsMap.get(requestMessage.getSenderName())
                                    .get(requestMessage.getReceiverOutputStreamId()).write(requestMessage.getBytes());

                            if (requestMessage.getSenderInputStreamId() == Message.FILE_IS_UPLOADED) {
                                closeAndRemoveStreamFromOutputStreamMap(requestMessage.getSenderName()
                                        , requestMessage.getReceiverOutputStreamId(), true);

                                messageHelper.writeInfoMessage(String.format("Файл: %s от пользователя: %s загружен"
                                        , requestMessage.getData(), requestMessage.getSenderName()));

                                connection.send(MessageFactory.getFileIsDownloadedResponseMessage(requestMessage));
                            } else {
                                connection.send(MessageFactory.getFileMessageResponse(requestMessage));
                            }
                        }
                    }
                }
                catch (IOException e) {
                    clientConnected = false;
                }
            }
        }

        protected void processFileMessageResponse(Message responseMessage) {
            if (responseMessage.getReceiverOutputStreamId() == Message.FILE_TRANSFER_ERROR) {
                closeAndRemoveStreamFromInputStreamsMap(responseMessage.getReceiverName()
                        , responseMessage.getSenderInputStreamId());
            } else if (responseMessage.getReceiverOutputStreamId() == Message.FILE_IS_DOWNLOADED) {
                messageHelper.writeInfoMessage(String.format("Пользователь: %s получил файл: %s"
                        , responseMessage.getReceiverName(), responseMessage.getData()));
            } else {
                synchronized (closeAndRemoveFromInputStreamsMapLock) {
                    if (inputStreamsMap.containsKey(responseMessage.getReceiverName()) &&
                            inputStreamsMap.get(responseMessage.getReceiverName())
                                    .containsKey(responseMessage.getSenderInputStreamId())) {
                        FileInputStream inputStream = inputStreamsMap.get(responseMessage.getReceiverName())
                                .get(responseMessage.getSenderInputStreamId());

                        try {
                            if(inputStream.available() > 1024*1024) {
                                byte[] bytes = new byte[1024*1024];
                                inputStream.read(bytes, 0, bytes.length);
                                connection.send(MessageFactory.getFileMessageRequest(responseMessage, bytes));
                            } else if (inputStream.available() > 0){
                                byte[] bytes = new byte[inputStream.available()];
                                inputStream.read(bytes, 0, bytes.length);

                                connection.send(MessageFactory.getFileIsUploadedRequestMessage(responseMessage, bytes));

                                closeAndRemoveStreamFromInputStreamsMap(responseMessage.getReceiverName()
                                        , responseMessage.getSenderInputStreamId());
                            }
                        } catch (IOException e) {
                            clientConnected = false;
                        }
                    }
                }
            }
        }

        protected void informAboutAddingNewUser(String userName) {
            messageHelper.writeInfoMessage("Участник с именем " + userName + " присоединился к чату");
        }

        protected void informAboutDeletingNewUser(String userName) {
            closeAndRemoveAllReceiverStreamsFromInputStreamsMap(userName, true);
            closeAndRemoveAllSenderStreamsFromOutputStreamsMap(userName, true);

            messageHelper.writeInfoMessage("Участник с именем " + userName + " покинул чат");
        }

        protected void notifyConnectionStatusChanged(boolean clientConnected) {
            Client.this.clientConnected = clientConnected;

            synchronized (Client.this) {
                Client.this.notify();
            }
        }

        protected void clientHandshake() throws IOException, ClassNotFoundException {
            while (true) {
                Message message = connection.receive();

                if (message.getType() == (MessageType.NAME_REQUEST)) {
                    connection.send(MessageFactory.getUserNameMessage(getUserName()));
                } else if (message.getType() == (MessageType.NAME_ACCEPTED)) {
                    notifyConnectionStatusChanged(true);
                    break;
                } else if (message.getType() == (MessageType.ERROR_MESSAGE)) {
                    processErrorMessage(message.getData());
                } else {
                    throw new IOException("Unexpected MessageType");
                }
            }
        }

        protected void clientMainLoop() throws IOException, ClassNotFoundException {
            while (clientConnected) {
                Message message = connection.receive();

                new Thread() {
                    @Override
                    public void run() {
                        MessageType messageType = message.getType();

                        if (messageType == MessageType.FILE_MESSAGE_REQUEST) {
                            processFileMessageRequest(message);
                        } else if (messageType == MessageType.FILE_MESSAGE_RESPONSE) {
                            processFileMessageResponse(message);
                        } else if (messageType == MessageType.TEXT_MESSAGE) {
                            processIncomingMessage(message.getData());
                        } else if (messageType == MessageType.ERROR_MESSAGE) {
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
                }.start();
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