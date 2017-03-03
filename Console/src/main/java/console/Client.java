package console;

import connection.Connection;
import connection.impl.ConnectionImpl;
import messages.Message;
import messages.MessageFactory;
import server.*;

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
public class Client{
    protected Connection connection;
    protected volatile boolean clientConnected = false;
    private final Object fileRequestLock = new Object();
    private final Object closeAndRemoveFromInputStreamsMapLock = new Object();
    private final Object closeAndRemoveFromOutputStreamMapLock = new Object();

    //temporarily start

    public Map<String, Map<Integer, FileInputStream>> getInputStreamsMap() {
        return inputStreamsMap;
    }

    public void setInputStreamsMap(Map<String, Map<Integer, FileInputStream>> inputStreamsMap) {
        this.inputStreamsMap = inputStreamsMap;
    }

    public Map<String, Map<Integer, FileOutputStream>> getOutputStreamsMap() {
        return outputStreamsMap;
    }

    public void setOutputStreamsMap(Map<String, Map<Integer, FileOutputStream>> outputStreamsMap) {
        this.outputStreamsMap = outputStreamsMap;
    }

    //temporarily end

    protected Map<String, Map<Integer, FileInputStream>> inputStreamsMap = new ConcurrentHashMap<>();
    protected Map<String, Map<Integer, FileOutputStream>> outputStreamsMap = new ConcurrentHashMap<>();

    private AtomicInteger id = new AtomicInteger(1);

    protected Integer getStreamId() {
        return id.getAndIncrement();
    }

    protected String getServerAddress() {
        ConsoleHelper.writeMessage("Введите адрес сервера");
        return ConsoleHelper.readString();
    }

    protected int getServerPort() {
        return ConsoleHelper.readInt();
    }

    protected String getUserName() {
        ConsoleHelper.writeMessage("Введите имя пользователя");
        return ConsoleHelper.readString();
    }

    protected void writeMessage(String message) {
        ConsoleHelper.writeMessage(message);
    }

    protected boolean shouldSentTextFromConsole() {
        return true;
    }

    protected SocketThread getSocketThread() {
        return new SocketThread();
    }

    protected void sendTextMessage(String textMessage) {
        try {
            connection.send(MessageFactory.getTextMessage(textMessage));
        } catch (IOException e) {
            clientConnected = false;
        }
    }

    protected void createPrivateMessage (){
        ConsoleHelper.writeMessage("Введите точное имя получателя");
        String receiverName = ConsoleHelper.readString();

        ConsoleHelper.writeMessage("Наберите текст для приватного сообщения");
        String privateMessage = ConsoleHelper.readString();

        sendPrivateMessage(privateMessage, receiverName);
    }

    protected void sendPrivateMessage(String privateMessage, String receiverName) {
        try {
            connection.send(MessageFactory.getPrivateMessage(privateMessage, receiverName));
        } catch (IOException e) {
            clientConnected = false;
        }
    }

    protected void createFileMessageForAll() {
        writeMessage("Введите полный путь к файлу, файл не должен превышать 10 мегобайт");

        sendFileMessageForAll(new File(ConsoleHelper.readString()));
    }

    protected void sendFileMessageForAll(File file) {
        if (!file.isFile()) {
            writeMessage("Такого файла не существует");
        } else if (file.length() > 1024*1024*10) {
            writeMessage("Файл не должен превышать 10 мегобайт");
        } else {
            try (BufferedInputStream inputStream = new BufferedInputStream(new FileInputStream(file))){
                byte[] bytes = new byte[inputStream.available()];
                inputStream.read(bytes);

                connection.send(MessageFactory.getFileMessageForAll(file.getName(), bytes));

                writeMessage("Файл отправлен");
            } catch (FileNotFoundException e) {
                writeMessage("Ошибка чтения файла");
            } catch (IOException e) {
                clientConnected = false;
            }
        }
    }

    protected void createFileMessage() {
        ConsoleHelper.writeMessage("Введите точное имя получателя");

        String receiverName = ConsoleHelper.readString();

        ConsoleHelper.writeMessage("Введите полный путь к файлу");

        File file = new File(ConsoleHelper.readString());

        if (file.isFile()) {
            sendFileMessage(receiverName, file);
        } else {
            ConsoleHelper.writeMessage("Такого файла не существует");
        }
    }

    protected void sendFileMessage(String receiverName, File file) {
        try {
            FileInputStream inputStream = new FileInputStream(file);

            Integer id = getStreamId();

            Map<Integer, FileInputStream> map = new HashMap<>();
            map.put(id, inputStream);

            inputStreamsMap.put(receiverName, map);

            connection.send(MessageFactory.getFileMessage(file.getName(), receiverName, id));
        } catch (FileNotFoundException e) {
            writeMessage("Ошибка чтения файла");
        } catch (IOException e) {
            clientConnected = false;
        }
    }

    private boolean askForExit() {
        while (true) {
            if (inputStreamsMap.size() > 0 && outputStreamsMap.size() > 0) {
                ConsoleHelper.writeMessage("У вас есть незавершенный прием и передача файлов, вы хотите прервать их? yes / no");
            } else if (inputStreamsMap.size() > 0) {
                ConsoleHelper.writeMessage("У вас есть незавершенная передача файлов, вы хотите прервать ее? yes / no");
            } else if (outputStreamsMap.size() > 0) {
                ConsoleHelper.writeMessage("У вас есть незавершенный прием файлов, вы хотите прервать его? yes / no");
            } else {
                return true;
            }

            String answer = ConsoleHelper.readString();
            if (answer.equals("yes")) {
                return true;
            } else if (answer.equals("no")) {
                return false;
            }
        }
    }

    public void run() {
        SocketThread socketThread = getSocketThread();
        socketThread.setDaemon(true);
        socketThread.start();

        synchronized (this) {
            try {
                wait();

                if (clientConnected) {
                    ConsoleHelper.writeMessage("Соединение установлено. Для выхода наберите команду 'exit'," +
                            " наберите 'pm' для отправки приватного сообщения," +
                            " наберите fm для отправки файла одному из пользывателей," +
                            " или fmfa для всех пользователей.");

                    runConsoleInputMessagesHandler();
                } else {
                    ConsoleHelper.writeMessage("Произошла ошибка во время работы клиента.");
                }
            } catch (InterruptedException e) {
                clientConnected = false;
            }
        }
    }

    protected void runConsoleInputMessagesHandler() throws InterruptedException {
        while (true) {
            if (clientConnected) {
                synchronized (fileRequestLock) {
                    String s = ConsoleHelper.readString();
                    if (s.equals("exit")) {
                        if (askForExit()) {
                            closeAndRemoveAllStreams(false);
                            break;
                        }
                    } else if (s.equals("pm")) {
                        createPrivateMessage();
                    } else if (s.equals("fmfa")) {
                        createFileMessageForAll();
                    } else if (s.equals("fm")) {
                        createFileMessage();
                    } else if (shouldSentTextFromConsole()) {
                        sendTextMessage(s);
                    }

                    fileRequestLock.wait(1);
                }
            } else {
                ConsoleHelper.writeMessage("Произошла ошибка во время работы клиента соединение с сервером утеряно.");
                closeAndRemoveAllStreams(true);
                break;
            }
        }
    }

    public static void main(String[] args) {
        Client client = new Client();
        client.run();
    }

    protected boolean askGetFile (String senderName, String fileName) {
        while (true) {
            String questionMessage = String.format("Пользователь %s отправил вам файл %s," +
                            " для получения введите yes для отказа введите no",
                    senderName, fileName);
            ConsoleHelper.writeMessage(questionMessage);

            String answer = ConsoleHelper.readString();

            switch (answer) {
                case "yes":
                    return true;
                case "no":
                    return false;
            }
        }
    }

    protected File getDirectoryFile () {
        while (true) {
            ConsoleHelper.writeMessage("Введите полный путь к папке для сохранения файла");

            File file = new File(ConsoleHelper.readString());

            if (file.isDirectory()) {
                return file;
            } else {
                ConsoleHelper.writeMessage("Вы ввели неправельный путь, повторите еще раз!");
            }
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
                            writeMessage(String.format("Ошибка передачи файла: %s учаснику: %s", file.getName(), receiverName));
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
                            writeMessage(String.format("Ошибка приема файла: %s от учасника: %s", file.getName(), senderName));
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

    public class SocketThread extends Thread {
        protected void processIncomingMessage(String message) {
            ConsoleHelper.writeMessage(message);
        }

        protected void processSystemMessage(String systemMessage) {
            ConsoleHelper.writeMessage("\u001B[34m" + systemMessage + "\u001B[0m");
        }

        protected void processErrorMessage(String errorMessage) {
            ConsoleHelper.writeMessage("\u001B[31m" + errorMessage + "\u001B[0m");
        }

        protected void processIncomingFileMessageForAll(Message fileMessageForAll) {
            synchronized (fileRequestLock) {
                if (askGetFile(fileMessageForAll.getSenderName(), fileMessageForAll.getData())) {
                    File file = getDirectoryFile();

                    try (FileOutputStream outputStream = new FileOutputStream(file.getPath() + File.separator + fileMessageForAll.getData())) {
                        outputStream.write(fileMessageForAll.getBytes());

                        ConsoleHelper.writeMessage("Файл сохранен");
                    } catch (IOException e) {
                        ConsoleHelper.writeMessage("Ощибка сохранения файла");
                    }
                }
            }
        }

        protected void processIncomingFileMessage(Message fileMessage) {
            synchronized (fileRequestLock) {
                if (askGetFile(fileMessage.getSenderName(), fileMessage.getData())) {
                    File file = getDirectoryFile();

                    try {
                        FileOutputStream outputStream
                                = new FileOutputStream(file.getPath() + File.separator + fileMessage.getData());

                        Integer id = getStreamId();

                        Map<Integer, FileOutputStream> map = new HashMap<>();
                        map.put(id, outputStream);
                        outputStreamsMap.put(fileMessage.getSenderName(), map);

                        connection.send(MessageFactory.getFileMessageAnswer(fileMessage, id));

                        ConsoleHelper.writeMessage("Процес получения файла " + fileMessage.getData());
                    } catch (FileNotFoundException e) {
                        try {
                            connection.send(MessageFactory.getFileTransferErrorResponseMessageFromClient(fileMessage));
                        } catch (IOException e1) {
                            clientConnected = false;
                        }
                        ConsoleHelper.writeMessage("Ощибка сохранения файла");
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
        }

        protected void processFileMessageRequest(Message requestMessage) {
            if (requestMessage.getSenderInputStreamId() != Message.FILE_TRANSFER_ERROR) {
                try {
                    synchronized (closeAndRemoveFromOutputStreamMapLock) {
                        if (outputStreamsMap.containsKey(requestMessage.getSenderName()) &&
                                outputStreamsMap.get(requestMessage.getSenderName())
                                        .containsKey(requestMessage.getReceiverOutputStreamId())) {
                            outputStreamsMap.get(requestMessage.getSenderName())
                                    .get(requestMessage.getReceiverOutputStreamId()).write(requestMessage.getBytes());

                            if (requestMessage.getSenderInputStreamId() != Message.FILE_IS_UPLOADED) {
                                connection.send(MessageFactory.getFileMessageResponse(requestMessage));
                            } else {
                                closeAndRemoveStreamFromOutputStreamMap(requestMessage.getSenderName()
                                        , requestMessage.getReceiverOutputStreamId(), true);

                                writeMessage(String.format("Файл: %s от пользователя: %s загружен"
                                        , requestMessage.getData(), requestMessage.getSenderName()));

                                connection.send(MessageFactory.getFileIsDownloadedResponseMessage(requestMessage));
                            }
                        }
                    }
                }
                catch (IOException e) {
                    clientConnected = false;
                }
            }
            else {
                closeAndRemoveStreamFromOutputStreamMap(requestMessage.getSenderName(), requestMessage.getReceiverOutputStreamId(), false);
            }
        }

        protected void processFileMessageResponse(Message responseMessage) {
            if (responseMessage.getReceiverOutputStreamId() == Message.FILE_TRANSFER_ERROR) {
                closeAndRemoveStreamFromInputStreamsMap(responseMessage.getReceiverName()
                        , responseMessage.getSenderInputStreamId());
            } else if (responseMessage.getReceiverOutputStreamId() == Message.FILE_IS_DOWNLOADED) {
                writeMessage(String.format("Пользователь: %s получил файл: %s"
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
            ConsoleHelper.writeMessage("Участник с именем " + userName + " присоединился к чату");
        }

        protected void informAboutDeletingNewUser(String userName) {
            closeAndRemoveAllReceiverStreamsFromInputStreamsMap(userName, true);
            closeAndRemoveAllSenderStreamsFromOutputStreamsMap(userName, true);

            ConsoleHelper.writeMessage("Участник с именем " + userName + " покинул чат");
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