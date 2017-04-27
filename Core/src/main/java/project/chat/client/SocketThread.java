package project.chat.client;

import project.chat.connection.impl.ConnectionImpl;
import project.chat.messages.Message;

import java.io.*;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;

import static project.chat.messages.MessageType.*;

/**
 * Created by s.sergienko on 24.04.2017.
 */
public abstract class SocketThread extends Thread{
    protected final Client client;

    protected SocketThread(Client client) {
        this.client = client;
    }

    protected abstract void processIncomingMessage(String message);

    protected abstract void informAboutAddingNewUser(String userName);

    protected abstract void informAboutDeletingNewUser(String userName);

    protected abstract void notifyConnectionStatusChanged(boolean clientConnected);

    protected void processInfoMessage(String infoMessage) {
        client.writeInfoMessage(infoMessage);
    }

    protected void processErrorMessage(String errorMessage) {
        client.writeErrorMessage(errorMessage);
    }

    protected void processIncomingFileMessageForAll(Message fileMessageForAll) {
        if (client.askGetFile(fileMessageForAll.getSenderName(), fileMessageForAll.getData())) {
            File file = client.getDirectoryFile();

            try (BufferedOutputStream outputStream = new BufferedOutputStream(
                    new FileOutputStream(file.getPath() + File.separator + fileMessageForAll.getData()))) {

                outputStream.write(fileMessageForAll.getBytes());
                outputStream.flush();

                client.writeInfoMessage("File saved");
            } catch (IOException e) {
                client.writeErrorMessage("Error saving file");
            }
        }
    }

    protected void processIncomingFileMessage(Message fileMessage) {
        if (client.askGetFile(fileMessage.getSenderName(), fileMessage.getData())) {
            try {
                synchronized (client.closeAndRemoveFromOutputStreamMapLock) {
                    FileOutputStream outputStream
                            = new FileOutputStream(client.getDirectoryFile().getPath() +
                            File.separator + fileMessage.getData());
                    Integer id = client.getStreamId();
                    String senderName = fileMessage.getSenderName();

                    if (client.outputStreamsMap.containsKey(senderName)) {
                        client.outputStreamsMap.get(senderName).put(id, outputStream);
                    } else {
                        Map<Integer, FileOutputStream> map = new HashMap<>();
                        map.put(id, outputStream);
                        client.outputStreamsMap.put(senderName, map);
                    }

                    client.writeInfoMessage("File obtaining process " + fileMessage.getData());

                    fileMessage.setType(FILE_MESSAGE_RESPONSE);
                    fileMessage.setReceiverOutputStreamId(id);
                }
            } catch (FileNotFoundException e) {
                client.writeErrorMessage("Error saving file " + fileMessage.getData());

                fileMessage.setType(FILE_DOWNLOAD_ERROR);
            }
        } else {
            fileMessage.setType(FILE_CANCEL);
        }

        try {
            client.connection.send(fileMessage);
        } catch (IOException e) {
            client.clientConnected = false;
        }
    }

    protected void processIncomingFileMessageRequestAndUpload(Message fileMessage) {
        synchronized (client.closeAndRemoveFromOutputStreamMapLock) {
            if (client.outputStreamsMap.containsKey(fileMessage.getSenderName()) &&
                    client.outputStreamsMap.get(fileMessage.getSenderName())
                            .containsKey(fileMessage.getReceiverOutputStreamId())) {
                try {
                    BufferedOutputStream outputStream = new BufferedOutputStream(client.outputStreamsMap.
                            get(fileMessage.getSenderName())
                            .get(fileMessage.getReceiverOutputStreamId()));

                    outputStream.write(fileMessage.getBytes());
                    outputStream.flush();

                    if (fileMessage.getType() == FILE_IS_UPLOADED) {
                        client.closeAndRemoveStreamFromOutputStreamMap(fileMessage.getSenderName()
                                , fileMessage.getReceiverOutputStreamId(), true);

                        client.writeInfoMessage(String.format("File %s from user %s is downloaded "
                                , fileMessage.getData(), fileMessage.getSenderName()));

                        fileMessage.setType(FILE_IS_DOWNLOADED);
                        fileMessage.setBytes(null);
                    } else {

                        fileMessage.setType(FILE_MESSAGE_RESPONSE);
                        fileMessage.setBytes(null);
                    }
                } catch (IOException e) {
                    client.writeErrorMessage(String.format("Error writing file %s from user %s"
                            , fileMessage.getData(), fileMessage.getSenderName()));

                    client.closeAndRemoveStreamFromOutputStreamMap(fileMessage.getSenderName()
                            , fileMessage.getReceiverOutputStreamId(), false);

                    fileMessage.setType(FILE_DOWNLOAD_ERROR);
                    fileMessage.setBytes(null);
                }

                try {
                    client.connection.send(fileMessage);
                } catch (IOException e) {
                    client.clientConnected = false;
                }
            }
        }
    }

    protected void processIncomingFileUploadError(Message fileMessage) {
        client.closeAndRemoveStreamFromOutputStreamMap(fileMessage.getSenderName()
                , fileMessage.getReceiverOutputStreamId(), false);
    }

    protected void processIncomingFileMessageResponse(Message fileMessage) {
        synchronized (client.closeAndRemoveFromInputStreamsMapLock) {
            if (client.inputStreamsMap.containsKey(fileMessage.getReceiverName()) &&
                    client.inputStreamsMap.get(fileMessage.getReceiverName())
                            .containsKey(fileMessage.getSenderInputStreamId())) {
                try {
                    InputStream inputStream = client.inputStreamsMap.get(fileMessage.getReceiverName())
                            .get(fileMessage.getSenderInputStreamId());

                    if (inputStream.available() > 1024 * 6) {
                        byte[] bytes = new byte[1024 * 6];
                        inputStream.read(bytes, 0, bytes.length);

                        fileMessage.setType(FILE_MESSAGE_REQUEST);
                        fileMessage.setBytes(bytes);
                    } else {
                        byte[] bytes = new byte[inputStream.available()];
                        inputStream.read(bytes, 0, bytes.length);

                        client.closeAndRemoveStreamFromInputStreamsMap(fileMessage.getReceiverName()
                                , fileMessage.getSenderInputStreamId());

                        fileMessage.setType(FILE_IS_UPLOADED);
                        fileMessage.setBytes(bytes);
                    }
                } catch (IOException e) {
                    client.writeErrorMessage(String.format("Error reading file %s, for user %s"
                            , fileMessage.getData(), fileMessage.getReceiverName()));

                    client.closeAndRemoveStreamFromInputStreamsMap(fileMessage.getReceiverName()
                            , fileMessage.getSenderInputStreamId());

                    fileMessage.setType(FILE_UPLOAD_ERROR);
                }

                try {
                    client.connection.send(fileMessage);
                } catch (IOException e) {
                    client.clientConnected = false;
                }
            }
        }
    }

    protected void processIncomingFileCancelAndDownloadError(Message fileMessage) {
        client.closeAndRemoveStreamFromInputStreamsMap(fileMessage.getReceiverName()
                , fileMessage.getSenderInputStreamId());
    }

    protected void clientHandshake() throws IOException, ClassNotFoundException {
        while (!isInterrupted()) {
            Message message = client.connection.receive();

            if (message.getType() == CREDENTIALS_REQUEST) {

                message.setType(USER_CREDENTIALS);
                message.setSenderName(client.getUserName());
                message.setData(client.getUserPassword());
                client.connection.send(message);
            } else if (message.getType() == USER_ACCEPTED) {
                notifyConnectionStatusChanged(true);
                break;
            } else if (message.getType() == ERROR_MESSAGE) {
                processErrorMessage(message.getData());
            } else {
                throw new IOException("Unexpected MessageType");
            }
        }
    }

    protected void clientMainLoop() throws IOException, ClassNotFoundException {
        while (client.clientConnected) {
            Message message = client.connection.receive();

            switch (message.getType()) {
                case FILE_MESSAGE_REQUEST:
                    processIncomingFileMessageRequestAndUpload(message);
                    break;
                case FILE_MESSAGE_RESPONSE:
                    processIncomingFileMessageResponse(message);
                    break;
                case TEXT_MESSAGE:
                    processIncomingMessage(message.getData());
                    break;
                case INFO_MESSAGE:
                    processInfoMessage(message.getData());
                    break;
                case ERROR_MESSAGE:
                    processErrorMessage(message.getData());
                    break;
                case USER_ADDED:
                    informAboutAddingNewUser(message.getData());
                    break;
                case USER_REMOVED:
                    informAboutDeletingNewUser(message.getData());
                    break;
                case FILE_MESSAGE:
                    processIncomingFileMessage(message);
                    break;
                case FILE_MESSAGE_FOR_ALL:
                    processIncomingFileMessageForAll(message);
                    break;
                case FILE_IS_UPLOADED:
                    processIncomingFileMessageRequestAndUpload(message);
                    break;
                case FILE_CANCEL:
                    processIncomingFileCancelAndDownloadError(message);
                    break;
                case FILE_UPLOAD_ERROR:
                    processIncomingFileUploadError(message);
                    break;
                case FILE_DOWNLOAD_ERROR:
                    processIncomingFileCancelAndDownloadError(message);
                    break;
                default:
                    client.clientConnected = false;
                    throw new RuntimeException("Unexpected MessageType: " + message.getType());
            }
        }
    }

    @Override
    public void run() {
        String address = client.getServerAddress();
        int port = client.getServerPort();

        try {
            Socket socket = new Socket(address, port);

            client.connection = new ConnectionImpl(socket);

            clientHandshake();

            clientMainLoop();
        } catch (IOException | ClassNotFoundException e) {
            client.closeAndRemoveAllStreams(true);
            notifyConnectionStatusChanged(false);
        }
    }
}
