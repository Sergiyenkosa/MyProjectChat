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
import static messages.Message.MessageType.*;

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

    public void sendFileMessageRequest(Message message) throws IOException {
        try {
            Connection receiverConnection = connectionMap.get(message.getReceiverName());
            receiverConnection.send(message);

            if (message.getSenderInputStreamId() == FILE_TRANSFER_ERROR) {
                String errorMessage = String.format("У пользователя: %s произошла ошибка чтения файла: %s",
                        message.getSenderName(), message.getData());

                receiverConnection.send(MessageFactory.getErrorMessage(errorMessage));
            }
        } catch (IOException | NullPointerException e) {
            log.error(e);
        }
    }

    public void sendFileMessageResponse(Message message) throws IOException {
        try {
            int receiverFileId = message.getReceiverOutputStreamId();
            Connection senderConnection = connectionMap.get(message.getSenderName());

            if (receiverFileId == FILE_IS_DOWNLOADED) {
                String infoMessage = String.format("Пользователь: %s получил файл: %s"
                        , message.getReceiverName(), message.getData());
                senderConnection.send(MessageFactory.getInfoMessage(infoMessage));
            } else if (receiverFileId == FILE_CANCEL || receiverFileId == FILE_TRANSFER_ERROR) {
                senderConnection.send(message);

                String cause = receiverFileId == Message.FILE_CANCEL ?
                        "Пользователь: %s отказался принять файл: %s" :
                        "У пользователя: %s произошла ошибка записи файла: %s";
                String errorMessage = String.format(cause,
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

        private String serverHandshake(Connection connection) throws IOException, ClassNotFoundException {
            while (true) {
                connection.send(MessageFactory.getCredentialsRequestMessage());

                Message message = connection.receive();

                if (message.getType() == USER_CREDENTIALS) {
                    UserDao userDao;

                    if ((userDao = storageDao.findByLogin(message.getSenderName())) == null) {
                        String errorMessage = "Wrong name, please try again :)";
                        connection.send(MessageFactory.getErrorMessage(errorMessage));
                    } else if(!userDao.getPassword().equals(message.getData())){
                        String errorMessage = "Wrong password, please try again :)";
                        connection.send(MessageFactory.getErrorMessage(errorMessage));
                    } else if (!connectionMap.containsKey(userDao.getLogin())) {
                        connectionMap.put(userDao.getLogin(), connection);

                        connection.send(MessageFactory.getUserAcceptedMessage());

                        return userDao.getLogin();
                    } else {
                        String errorMessage = "The user is already connected :(";
                        connection.send(MessageFactory.getErrorMessage(errorMessage));
                    }
                } else {
                    log.error("Unknown message type: " + message.getType());

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
                throws IOException, ClassNotFoundException, InterruptedException {
            while (true) {
                Message message = connection.receive();
                MessageType type = message.getType();

                if (type == TEXT_MESSAGE) {
                    if (isMessageTextCorrect(connection, message)) {
                        sendBroadcastMessage(MessageFactory
                                .getTextMessage(userName + ": " + message.getData()));
                    }
                } else if (type == PRIVATE_MESSAGE) {
                    if (isMessageTextCorrect(connection, message)) {
                        message.setSenderName(userName);
                        sendPrivateMessage(connection, message);
                    }
                } else if (type == FILE_MESSAGE_REQUEST) {
                    sendFileMessageRequest(message);
                } else if (type == FILE_MESSAGE_RESPONSE) {
                    sendFileMessageResponse(message);
                } else if (type == FILE_MESSAGE) {
                    message.setSenderName(userName);
                    sendFileMessage(connection, message);
                } else if (type == FILE_MESSAGE_FOR_ALL) {
                    message.setSenderName(userName);
                    sendFileMessageForAll(message);
                } else {
                    log.error("Message type error: " + type);
                    break;
                }

                Thread.sleep(5);
            }
        }

        @Override
        public void run() {
            log.info("установлено новое соединение с удаленным адресом: "
                    + socket.getRemoteSocketAddress());

            String userName = "";

            try (ConnectionImpl connection = new ConnectionImpl(socket)){
                if (!(userName = serverHandshake(connection)).equals("")) {
                    sendBroadcastMessage(MessageFactory.getUserAddedMessage(userName));

                    sendListOfUsers(connection);

                    serverMainLoop(connection, userName);
                }
            } catch (Exception e) {
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