package server;

import connection.Connection;
import connection.impl.ConnectionImpl;
import daos.StorageDao;
import daos.UserDao;
import messages.Message;
import messages.MessageFactory;
import org.apache.log4j.Logger;
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

    private boolean isMessageTextCorrect(Connection connection, Message message) throws IOException {
        if (textMessageValidator.isTextMessageCorrect(message.getData())) {
            return true;
        } else {
            String errorMessage = ("Error, message can not be more than "
                    + textMessageValidator.getMaxTextLength()
                    + " characters long");

            message.setType(ERROR_MESSAGE);
            message.setData(errorMessage);
            message.setReceiverName(null);
            connection.send(message);

            return false;
        }
    }

    private void sendBroadcastMessage(Message message) {
        for (Map.Entry<String, Connection> entry : connectionMap.entrySet()) {
            try {
                entry.getValue().send(message);
            } catch (IOException e) {
                log.error("Remote address  communicating error: "
                        + entry.getValue().getRemoteSocketAddress(), e);
            }
        }
    }

    private void sendPrivateMessage(Connection connection, Message message) throws IOException {
        try {
            String privateMessage = String.format("Private message from %s: %s"
                    , message.getSenderName(), message.getData());
            Connection receiverConnection = connectionMap.get(message.getReceiverName());

            message.setType(TEXT_MESSAGE);
            message.setData(privateMessage);
            message.setSenderName(null);

            receiverConnection.send(message);
        } catch (IOException | NullPointerException e) {
            String errorMessage = "Error, private message is not sent, user " +
                    message.getReceiverName() + " is unavailable.";

            message.setType(ERROR_MESSAGE);
            message.setData(errorMessage);
            message.setReceiverName(null);
            connection.send(message);
        }
    }

    private void sendFileMessageForAll(Message message) {
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

    private void sendFileMessage(Connection connection, Message message) throws IOException {
        if (message.getSenderName().equals(message.getReceiverName())) {

            message.setType(FILE_MESSAGE_RESPONSE);
            message.setSenderName(null);
            message.setReceiverOutputStreamId(FILE_TRANSFER_ERROR);

            connection.send(message);

            String errorMessage = "Error, file " + message.getData() +
                    " isn't sent, you can't send the file to yourself";

            message.setType(ERROR_MESSAGE);
            message.setData(errorMessage);
            message.setReceiverName(null);
            connection.send(message);
        } else {
            try {
                connectionMap.get(message.getReceiverName()).send(message);
            } catch (IOException | NullPointerException e) {
                message.setType(FILE_MESSAGE_RESPONSE);
                message.setSenderName(null);
                message.setReceiverOutputStreamId(FILE_TRANSFER_ERROR);

                connection.send(message);

                String errorMessage = String.format("Error, file %s is not sent, user %s is unavailable.",
                        message.getData(), message.getReceiverName());

                message.setType(ERROR_MESSAGE);
                message.setData(errorMessage);
                message.setReceiverName(null);
                connection.send(message);

                connection.send(message);
            }
        }
    }

    private void sendFileMessageRequest(Message message) throws IOException {
        try {
            Connection receiverConnection = connectionMap.get(message.getReceiverName());
            receiverConnection.send(message);

            if (message.getSenderInputStreamId() == FILE_TRANSFER_ERROR) {
                String errorMessage = String.format("Error reading file %s, on user %s side.",
                        message.getData(), message.getSenderName());

                message.setType(ERROR_MESSAGE);
                message.setData(errorMessage);
                message.setSenderName(null);
                message.setReceiverName(null);
                message.setBytes(null);

                receiverConnection.send(message);
            }
        } catch (IOException | NullPointerException e) {
            log.error(e);
        }
    }

    private void sendFileMessageResponse(Message message) throws IOException {
        try {
            int receiverFileId = message.getReceiverOutputStreamId();
            Connection senderConnection = connectionMap.get(message.getSenderName());

            if (receiverFileId == FILE_IS_DOWNLOADED) {
                String infoMessage = String.format("User %s has received a file %s"
                        , message.getReceiverName(), message.getData());

                message.setType(INFO_MESSAGE);
                message.setData(infoMessage);
                message.setSenderName(null);
                message.setReceiverName(null);
                senderConnection.send(message);
            } else if (receiverFileId == FILE_CANCEL || receiverFileId == FILE_TRANSFER_ERROR) {
                senderConnection.send(message);

                String cause = receiverFileId == Message.FILE_CANCEL ?
                        "User %s refused to accept file %s" :
                        "Error writing file %s, on user %s side";
                String errorMessage = String.format(cause,
                        message.getReceiverName(), message.getData());

                message.setType(ERROR_MESSAGE);
                message.setData(errorMessage);
                message.setSenderName(null);
                message.setReceiverName(null);

                senderConnection.send(message);
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

                        message.setType(ERROR_MESSAGE);
                        message.setData(errorMessage);
                        message.setSenderName(null);
                        connection.send(message);
                    } else if(!userDao.getPassword().equals(message.getData())){
                        String errorMessage = "Wrong password, please try again :)";

                        message.setType(ERROR_MESSAGE);
                        message.setData(errorMessage);
                        message.setSenderName(null);
                        connection.send(message);
                    } else if (!connectionMap.containsKey(userDao.getLogin())) {
                        connectionMap.put(userDao.getLogin(), connection);

                        message.setType(USER_ACCEPTED);
                        message.setData(null);
                        message.setSenderName(null);
                        connection.send(message);

                        return userDao.getLogin();
                    } else {
                        String errorMessage = "The user is already connected :(";

                        message.setType(ERROR_MESSAGE);
                        message.setData(errorMessage);
                        message.setSenderName(null);
                        connection.send(message);
                    }
                } else {
                    log.error("Unknown message type: " + message.getType());

                    throw new IOException();
                }
            }
        }

        private void sendListOfUsers(Connection connection) throws IOException {
            for (Map.Entry<String, Connection> entry : connectionMap.entrySet()) {
                if (connection != entry.getValue()) {
                    connection.send(MessageFactory.getUserAddedMessage(entry.getKey()));
                }
            }
        }

        private void serverMainLoop(Connection connection, String userName)
                throws IOException, ClassNotFoundException, InterruptedException {
            while (true) {
                Message message = connection.receive();
                MessageType type = message.getType();

                if (type == TEXT_MESSAGE) {
                    if (isMessageTextCorrect(connection, message)) {
                        message.setData(userName + ": " + message.getData());

                        sendBroadcastMessage(message);
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
            }
        }

        @Override
        public void run() {
            log.info("A new connection with a remote address "
                    + socket.getRemoteSocketAddress() + "is established");

            String userName = "";

            try (ConnectionImpl connection = new ConnectionImpl(socket)){
                if (!(userName = serverHandshake(connection)).equals("")) {
                    sendBroadcastMessage(MessageFactory.getUserAddedMessage(userName));

                    sendListOfUsers(connection);

                    serverMainLoop(connection, userName);
                }
            } catch (Exception e) {
                log.error("An error occurred while communicating with the remote address: "
                        + socket.getRemoteSocketAddress(), e);
            }

            if (!userName.equals("")) {
                connectionMap.remove(userName);

                sendBroadcastMessage(MessageFactory.getUserRemovedMessage(userName));
            }

            log.info("Connection to remote address " + socket.getRemoteSocketAddress() + " is closed");
        }
    }
}