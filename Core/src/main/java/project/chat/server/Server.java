package project.chat.server;

import org.apache.log4j.Logger;
import project.chat.connection.Connection;
import project.chat.connection.impl.ConnectionImpl;
import project.chat.dto.StorageDto;
import project.chat.dto.UserDto;
import project.chat.messages.Message;
import project.chat.messages.MessageFactory;
import project.chat.messages.MessageType;
import project.chat.server.validators.TextMessageValidator;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static project.chat.messages.MessageType.*;

/**
 * Created by s.sergienko on 11.10.2016.
 */
public class Server {
    private final ServerSocket serverSocket;
    private final TextMessageValidator textMessageValidator;
    private final StorageDto storageDto;
    private static final Logger log = Logger.getLogger(Server.class);
    private Map<String, Connection> connectionMap = new ConcurrentHashMap<>();

    public Server(ServerSocket serverSocket, TextMessageValidator textMessageValidator, StorageDto storageDto) {
        this.serverSocket = serverSocket;
        this.textMessageValidator = textMessageValidator;
        this.storageDto = storageDto;
    }

    public void runServer() {
        String serverCondition = "The server is running";
        log.info(serverCondition);

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

            message.setType(FILE_DOWNLOAD_ERROR);
            message.setSenderName(null);

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
                message.setType(FILE_DOWNLOAD_ERROR);
                message.setSenderName(null);
                connection.send(message);

                String errorMessage = String.format("Error, file %s is not sent, user %s is unavailable.",
                        message.getData(), message.getReceiverName());

                message.setType(ERROR_MESSAGE);
                message.setData(errorMessage);
                message.setReceiverName(null);
                connection.send(message);
            }
        }
    }

    private void sendFileMessageRequestAndUpload(Message message) throws IOException {
        try {
            connectionMap.get(message.getReceiverName()).send(message);
        } catch (IOException | NullPointerException e) {
            log.error(e);
        }
    }

    private void sendFileMessageUploadError(Message message) {
        try {
            Connection receiverConnection = connectionMap.get(message.getReceiverName());
            receiverConnection.send(message);


                String errorMessage = String.format("Error reading file %s, on user %s's side.",
                        message.getData(), message.getSenderName());

                message.setType(ERROR_MESSAGE);
                message.setData(errorMessage);
                message.setSenderName(null);
                message.setReceiverName(null);
                message.setBytes(null);

                receiverConnection.send(message);
        } catch (IOException | NullPointerException e) {
            log.error(e);
        }
    }

    private void sendFileMessageResponse(Message message) throws IOException {
        try {
            connectionMap.get(message.getSenderName()).send(message);
        } catch (IOException | NullPointerException e) {
            log.error(e);
        }
    }

    private void sendFileMessageDownload(Message message) {
        try {
            Connection senderConnection = connectionMap.get(message.getSenderName());

            String infoMessage = String.format("User %s has received a file %s"
                    , message.getReceiverName(), message.getData());

            message.setType(INFO_MESSAGE);
            message.setData(infoMessage);
            message.setSenderName(null);
            message.setReceiverName(null);
            senderConnection.send(message);
        } catch (IOException | NullPointerException e) {
            log.error(e);
        }
    }

    private void sendFileMessageCancelAndDownloadError(Message message) {
        try {
            Connection senderConnection = connectionMap.get(message.getSenderName());

            senderConnection.send(message);

            String cause = message.getType() == FILE_CANCEL ?
                    "User %s refused to accept file %s" :
                    "Error writing file %s, on user %s's side";
            String errorMessage = String.format(cause,
                    message.getReceiverName(), message.getData());

            message.setType(ERROR_MESSAGE);
            message.setData(errorMessage);
            message.setSenderName(null);
            message.setReceiverName(null);

            senderConnection.send(message);
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
                    UserDto user;

                    if ((user = storageDto.findByLogin(message.getSenderName())) == null) {
                        String errorMessage = "Wrong name, please try again :)";

                        message.setType(ERROR_MESSAGE);
                        message.setData(errorMessage);
                        message.setSenderName(null);
                        connection.send(message);
                    } else if(!user.getPassword().equals(message.getData())){
                        String errorMessage = "Wrong password, please try again :)";

                        message.setType(ERROR_MESSAGE);
                        message.setData(errorMessage);
                        message.setSenderName(null);
                        connection.send(message);
                    } else if (!connectionMap.containsKey(user.getLogin())) {
                        connectionMap.put(user.getLogin(), connection);

                        message.setType(USER_ACCEPTED);
                        message.setData(null);
                        message.setSenderName(null);
                        connection.send(message);

                        return user.getLogin();
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
                } else if (type == FILE_MESSAGE_REQUEST || type == FILE_IS_UPLOADED) {
                    sendFileMessageRequestAndUpload(message);
                } else if (type == FILE_MESSAGE_RESPONSE) {
                    sendFileMessageResponse(message);
                } else if (type == FILE_MESSAGE) {
                    message.setSenderName(userName);
                    sendFileMessage(connection, message);
                } else if (type == FILE_MESSAGE_FOR_ALL) {
                    message.setSenderName(userName);
                    sendFileMessageForAll(message);
                } else if (type == FILE_UPLOAD_ERROR) {
                    sendFileMessageUploadError(message);
                } else if (type == FILE_IS_DOWNLOADED) {
                    sendFileMessageDownload(message);
                } else if (type == FILE_CANCEL || type == FILE_DOWNLOAD_ERROR) {
                    sendFileMessageCancelAndDownloadError(message);
                }else {
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