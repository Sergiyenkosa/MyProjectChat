package server;

import connection.Connection;
import connection.impl.ConnectionImpl;
import messages.Message;
import messages.MessageFactory;
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
    private Map<String, Connection> connectionMap = new ConcurrentHashMap<>();

    public Server(ServerSocket serverSocket, TextMessageValidator textMessageValidator) {
        this.serverSocket = serverSocket;
        this.textMessageValidator = textMessageValidator;
    }

    public void runServer() {
        ConsoleHelper.writeMessage("The server is running");

        try {
            while (true) {
                new Handler(serverSocket.accept()).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        ClassPathXmlApplicationContext context
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
                ConsoleHelper.writeMessage("Произошла ошибка при обмене данными с удаленным адресом: "
                        + entry.getValue().getRemoteSocketAddress());
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
                    e.printStackTrace();
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
            sendFileMessageRequest(connection, message);
        }
    }

    public void sendFileMessageRequest(Connection connection, Message message) throws IOException {
        try {
            connectionMap.get(message.getReceiverName()).send(message);
        } catch (IOException | NullPointerException e) {
            connection.send(MessageFactory.getFileTransferErrorResponseMessageFromServer(message));

            String errorMessage = String.format("Ошибка, фаил: %s не отправлен, пользователь: " +
                    "%s отсутствует или покинул чат.", message.getData(), message.getReceiverName());
            connection.send(MessageFactory.getErrorMessage(errorMessage));
        }
    }

    public void sendFileMessageResponse(Connection connection, Message message) throws IOException {
        int receiverFileId = message.getReceiverOutputStreamId();

        try {
            Connection senderConnection = connectionMap.get(message.getSenderName());

            if (receiverFileId == FILE_CANCEL || receiverFileId == FILE_TRANSFER_ERROR) {
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
            if (receiverFileId == FILE_CANCEL || receiverFileId == FILE_TRANSFER_ERROR
                    || receiverFileId == FILE_IS_DOWNLOADED) {
                e.printStackTrace();
            } else {
                connection.send(MessageFactory.getFileTransferErrorRequestMessageFromServer(message));

                String errorMessage = String.format("Ошибка, фаил: %s не принят, пользователь: "
                        + "%s отсутствует или покинул чат.", message.getData(), message.getSenderName());
                connection.send(MessageFactory.getErrorMessage(errorMessage));
            }
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

                if (message.getType() == MessageType.USER_NAME) {
                    String name = message.getData();

                    if (connectionMap.containsKey(name)) {
                        String errorMessage = "Пользователь с таким именем уже подключен к чату.";
                        connection.send(MessageFactory.getErrorMessage(errorMessage));
                    } else if (name.matches("^\\w{1,20}$")) {
                        connectionMap.put(name, connection);

                        connection.send(MessageFactory.getNameAcceptedMessage());

                        return name;
                    } else {
                        String errorMessage = name.isEmpty() ? "Error: The name is empty, try again."
                                : "Error: The name contains illegal chars, try again.";
                        connection.send(MessageFactory.getErrorMessage(errorMessage));
                    }
                } else {
                    ConsoleHelper.writeMessage("Ошибка формата сообщения: " + message.getType());

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
                    e.printStackTrace();
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
                        ConsoleHelper.writeMessage("Ошибка формата сообщения: " + message.getType());
                }
            }
        }

        @Override
        public void run() {
            ConsoleHelper.writeMessage("установлено новое соединение с удаленным адресом: "
                    + socket.getRemoteSocketAddress());

            String userName = "";

            try (ConnectionImpl connection = new ConnectionImpl(socket)){//ask about closable
                userName = serverHandshake(connection);
                sendBroadcastMessage(MessageFactory.getUserAddedMessage(userName));

                sendListOfUsers(connection);

                serverMainLoop(connection, userName);
            } catch (IOException | ClassNotFoundException e) {
                ConsoleHelper.writeMessage("Произошла ошибка при обмене данными с удаленным адресом: "
                        + socket.getRemoteSocketAddress());
            }

            if (!userName.equals("")) {
                connectionMap.remove(userName);

                sendBroadcastMessage(MessageFactory.getUserRemovedMessage(userName));
            }

            ConsoleHelper.writeMessage("Соединение с удаленным адресом: "
                    + socket.getRemoteSocketAddress() + " закрыто");
        }
    }
}