package server;

import connection.Connection;
import connection.impl.ConnectionImpl;
import daos.StorageDao;
import daos.UserDao;
import messages.Message;
import messages.MessageFactory;
import org.apache.log4j.Logger;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import server.validators.TextMessageValidator;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static messages.Message.*;

/**
 * Created by s.sergienko on 11.10.2016.
 */
public class Server {
    private final ServerSocket serverSocket;
    private final TextMessageValidator textMessageValidator;
    private final StorageDao storageDao;
    private static final Logger log = Logger.getLogger(Server.class);
    private Map<String, Connection> connectionMap = new ConcurrentHashMap<>();

    public Server(ServerSocket serverSocket, TextMessageValidator textMessageValidator, StorageDao storageDao) {
        this.serverSocket = serverSocket;
        this.textMessageValidator = textMessageValidator;
        this.storageDao = storageDao;
    }

    public void runServer() {
        String serverCondition = "The server is running";

        System.out.println(serverCondition);
        log.info(serverCondition);
        System.out.println();

        try {
            while (true) {
                new Handler(serverSocket.accept()).start();
            }
        } catch (IOException e) {
            log.error(e);
        }
    }

    public static void main(String[] args) {
        ApplicationContext context
                = new ClassPathXmlApplicationContext("spring/core-context.xml");

        Server server = (Server) context.getBean("server");

        server.runServer();
    }

    public boolean isMessageTextCorrect(Connection connection, Message message) throws IOException {
        if (textMessageValidator.isTextMessageCorrect(message.getData())) {
            return true;
        } else {
            String errorMessage = ("Ошибка сообщение не должно привышать "
                    + textMessageValidator.getMaxTextLength()
                    + " символов.");
            connection.send(MessageFactory.getErrorMessage(errorMessage));

            return false;
        }
    }

    public void sendBroadcastMessage(Message message) {
        for (Map.Entry<String, Connection> entry : connectionMap.entrySet()) {
            try {
                entry.getValue().send(message);
            } catch (IOException e) {
                log.error("Произошла ошибка при обмене данными с удаленным адресом: "
                        + entry.getValue().getRemoteSocketAddress(), e);
            }
        }
    }

    public void sendPrivateMessage(Connection connection, Message message) throws IOException {
        try {
            String privateMessage = String.format("Private message from %s: %s"
                    , message.getSenderName(), message.getData());

            connectionMap.get(message.getReceiverName()).send(MessageFactory.getTextMessage(privateMessage));
        } catch (IOException | NullPointerException e) {
            String cause = e instanceof IOException ? "не доступен" : "отсутствует в чате";
            String errorMessage = String.format("Ошибка, приватное сообщение не отправлено,"
                    + " пользователь c именем: %s %s.", message.getReceiverName(), cause);
            connection.send(MessageFactory.getErrorMessage(errorMessage));
        }
    }

    public void sendFileMessageForAll(Message message) {
        for (Map.Entry<String, Connection> entry : connectionMap.entrySet()) {
            if (!entry.getKey().equals(message.getSenderName())) {
                try {
                    entry.getValue().send(message);
                } catch (IOException e) {
                    log.error(e);
                }
            }
        }
    }

    public void sendFileMessage(Connection connection, Message message) throws IOException {
        if (message.getSenderName().equals(message.getReceiverName())) {

            connection.send(MessageFactory.getFileTransferErrorResponseMessageFromServer(message));

            String errorMessage = "Server: Ошибка файл: " + message.getData()
                    + " не отправлен, вы не можете отправить файл самому себе";
            connection.send(MessageFactory.getErrorMessage(errorMessage));
        } else {
            try {
                connectionMap.get(message.getReceiverName()).send(message);
            } catch (IOException | NullPointerException e) {
                connection.send(MessageFactory.getFileTransferErrorResponseMessageFromServer(message));

                String errorMessage = String.format("Ошибка, фаил: %s не отправлен, пользователь: " +
                        "%s отсутствует или покинул чат.", message.getData(), message.getReceiverName());
                connection.send(MessageFactory.getErrorMessage(errorMessage));
            }
        }
    }

    public void sendFileMessageRequest(Connection connection, Message message) throws IOException {
        try {
            connectionMap.get(message.getReceiverName()).send(message);
        } catch (IOException | NullPointerException e) {
            log.error(e);
        }
    }

    public void sendFileMessageResponse(Connection connection, Message message) throws IOException {
        int receiverFileId = message.getReceiverOutputStreamId();

        try {
            Connection senderConnection = connectionMap.get(message.getSenderName());
            senderConnection.send(message);

            if (receiverFileId == FILE_IS_DOWNLOADED) {
                String infoMessage = String.format("Пользователь: %s получил файл: %s"
                        , message.getReceiverName(), message.getData());
                senderConnection.send(MessageFactory.getInfoMessage(infoMessage));
            } else if (receiverFileId == FILE_CANCEL || receiverFileId == FILE_TRANSFER_ERROR) {
                senderConnection.send(message);

                String cause = receiverFileId == Message.FILE_CANCEL
                        ? "Пользователь: %s отказался принять файл"
                        : "У пользователя: %s произошла ошибка сохранения файла";
                String errorMessage = String.format(cause + ": %s.",
                        message.getReceiverName(), message.getData());
                senderConnection.send(MessageFactory.getErrorMessage(errorMessage));
            } else {
                senderConnection.send(message);
            }
        } catch (IOException | NullPointerException e) {
            log.error(e);
        }
    }

    private class Handler extends Thread {
        private final Socket socket;

        private Handler(Socket socket) {
            this.socket = socket;
        }

        public String serverHandshake(Connection connection) throws IOException, ClassNotFoundException {
            while (true) {
                connection.send(MessageFactory.getNameRequestMessage());

                Message message = connection.receive();
                switch (message.getType()) {
                    case USER_NAME:
                        String name = message.getData();
                        UserDao userDao;

                        if (connectionMap.containsKey(name)) {
                            String errorMessage = "Пользователь с таким именем уже подключен к чату.";
                            connection.send(MessageFactory.getErrorMessage(errorMessage));
                            break;
                        } else if ((userDao = storageDao.findByLogin(name)) != null) {
                            connection.send(MessageFactory.getPasswordRequestMessage());

                            message = connection.receive();
                            if (message.getType() == MessageType.USER_PASSWORD) {
                                if (message.getData().equals(userDao.getPassword())) {
                                    connectionMap.put(name, connection);

                                    connection.send(MessageFactory.getNameAcceptedMessage());

                                    return name;
                                } else {
                                    String errorMessage = "Ошибка, неверный пароль";
                                    connection.send(MessageFactory.getErrorMessage(errorMessage));
                                    break;
                                }
                            }
                        } else {
                            String errorMessage = "Ошибка, такого пользователя не существует в базе данных";
                            connection.send(MessageFactory.getErrorMessage(errorMessage));
                            break;
                        }
                    default:
                        log.error("Ошибка формата сообщения: " + message.getType());

                        throw new IOException();
                }
            }
        }

        public void sendListOfUsers(Connection connection) throws IOException {
            for (Map.Entry<String, Connection> entry : connectionMap.entrySet()) {
                if (connection != entry.getValue()) {
                    connection.send(MessageFactory.getUserAddedMessage(entry.getKey()));
                }
            }
        }

        public void serverMainLoop(Connection connection, String userName)
                throws IOException, ClassNotFoundException {
            while (true) {
                Message message = connection.receive();

                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    log.error(e);
                }

                switch (message.getType()) {
                    case TEXT_MESSAGE:
                        if (isMessageTextCorrect(connection, message)) {
                            sendBroadcastMessage(MessageFactory
                                    .getTextMessage(userName + ": " + message.getData()));
                        }
                        break;
                    case PRIVATE_MESSAGE:
                        if (isMessageTextCorrect(connection, message)) {
                            message.setSenderName(userName);
                            sendPrivateMessage(connection, message);
                        }
                        break;
                    case FILE_MESSAGE_REQUEST:
                        sendFileMessageRequest(connection, message);
                        break;
                    case FILE_MESSAGE_RESPONSE:
                        sendFileMessageResponse(connection, message);
                        break;
                    case FILE_MESSAGE:
                        message.setSenderName(userName);
                        sendFileMessage(connection, message);
                        break;
                    case FILE_MESSAGE_FOR_ALL:
                        message.setSenderName(userName);
                        sendFileMessageForAll(message);
                        break;
                    default:
                        log.error("Ошибка формата сообщения: " + message.getType());
                }
            }
        }

        @Override
        public void run() {
            log.info("установлено новое соединение с удаленным адресом: "
                    + socket.getRemoteSocketAddress());

            String userName = "";

            try (ConnectionImpl connection = new ConnectionImpl(socket)){//ask about closable
                userName = serverHandshake(connection);
                sendBroadcastMessage(MessageFactory.getUserAddedMessage(userName));

                sendListOfUsers(connection);

                serverMainLoop(connection, userName);
            } catch (IOException | ClassNotFoundException e) {
                log.error("Произошла ошибка при обмене данными с удаленным адресом: "
                        + socket.getRemoteSocketAddress(), e);
            }

            if (!userName.equals("")) {
                connectionMap.remove(userName);

                sendBroadcastMessage(MessageFactory.getUserRemovedMessage(userName));
            }

            log.info("Соединение с удаленным адресом: "
                    + socket.getRemoteSocketAddress() + " закрыто");
        }
    }
}