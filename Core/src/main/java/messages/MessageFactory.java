package messages;

import static messages.Message.*;
import static messages.Message.MessageType.*;

/**
 * Created by s.sergienko on 01.03.2017.
 */
public class MessageFactory {
    public static Message getCredentialsRequestMessage() {
        Message message = new Message();
        message.setType(CREDENTIALS_REQUEST);
        return message;
    }

        public static Message getCredentialsMessage(String userName, String userPassword) {

        Message message = new Message();
        message.setType(USER_CREDENTIALS);
        message.setData(userPassword);
        message.setSenderName(userName);
        return message;
    }

    public static Message getUserAcceptedMessage() {
        Message message = new Message();
        message.setType(USER_ACCEPTED);
        return message;
    }

    public static Message getUserAddedMessage(String userName) {
        Message message = new Message();
        message.setType(USER_ADDED);
        message.setData(userName);
        return message;
    }

    public static Message getUserRemovedMessage(String userName) {
        Message message = new Message();
        message.setType(USER_REMOVED);
        message.setData(userName);
        return message;
    }

    public static Message getInfoMessage(String infoMessage) {
        Message message = new Message();
        message.setType(INFO_MESSAGE);
        message.setData(infoMessage);
        return message;
    }

    public static Message getErrorMessage(String errorMessage) {
        Message message = new Message();
        message.setType(ERROR_MESSAGE);
        message.setData(errorMessage);
        return message;
    }

    public static Message getTextMessage(String textMessage) {
        Message message = new Message();
        message.setType(TEXT_MESSAGE);
        message.setData(textMessage);
        return message;
    }

    public static Message getPrivateMessage(String privateMessage, String receiverName) {
        Message message = new Message();
        message.setType(PRIVATE_MESSAGE);
        message.setData(privateMessage);
        message.setReceiverName(receiverName);
        return message;
    }

    public static Message getFileMessageForAll(String fileName, byte[] bytes) {
        Message message = new Message();
        message.setType(FILE_MESSAGE_FOR_ALL);
        message.setData(fileName);
        message.setBytes(bytes);
        return message;
    }

    public static Message getFileMessage(String fileName, String receiverName, int senderInputStreamId) {
        Message message = new Message();
        message.setType(FILE_MESSAGE);
        message.setData(fileName);
        message.setReceiverName(receiverName);
        message.setSenderInputStreamId(senderInputStreamId);
        return message;
    }

    public static Message getFileMessageAnswer(Message fileMessage, int receiverOutputStreamId) {
        Message message = new Message();
        message.setType(FILE_MESSAGE_RESPONSE);
        message.setData(fileMessage.getData());
        message.setSenderName(fileMessage.getSenderName());
        message.setReceiverName(fileMessage.getReceiverName());
        message.setSenderInputStreamId(fileMessage.getSenderInputStreamId());
        message.setReceiverOutputStreamId(receiverOutputStreamId);
        return message;
    }

    public static Message getFileMessageRequest(Message responseMessage, byte[] bytes) {
        Message message = new Message();
        message.setType(FILE_MESSAGE_REQUEST);
        message.setData(responseMessage.getData());
        message.setSenderName(responseMessage.getSenderName());
        message.setReceiverName(responseMessage.getReceiverName());
        message.setSenderInputStreamId(responseMessage.getSenderInputStreamId());
        message.setReceiverOutputStreamId(responseMessage.getReceiverOutputStreamId());
        message.setBytes(bytes);
        return message;
    }

    public static Message getFileMessageResponse(Message requestMessage) {
        Message message = new Message();
        message.setType(FILE_MESSAGE_RESPONSE);
        message.setData(requestMessage.getData());
        message.setSenderName(requestMessage.getSenderName());
        message.setReceiverName(requestMessage.getReceiverName());
        message.setSenderInputStreamId(requestMessage.getSenderInputStreamId());
        message.setReceiverOutputStreamId(requestMessage.getReceiverOutputStreamId());
        return message;
    }

    public static Message getFileIsUploadedRequestMessage(Message responseMessage, byte[] bytes) {
        Message message = new Message();
        message.setType(FILE_MESSAGE_REQUEST);
        message.setData(responseMessage.getData());
        message.setSenderName(responseMessage.getSenderName());
        message.setReceiverName(responseMessage.getReceiverName());
        message.setSenderInputStreamId(FILE_IS_UPLOADED);
        message.setReceiverOutputStreamId(responseMessage.getReceiverOutputStreamId());
        message.setBytes(bytes);
        return message;
    }

    public static Message getFileIsDownloadedResponseMessage(Message requestMessage) {
        Message message = new Message();
        message.setType(FILE_MESSAGE_RESPONSE);
        message.setData(requestMessage.getData());
        message.setSenderName(requestMessage.getSenderName());
        message.setReceiverName(requestMessage.getReceiverName());
        message.setSenderInputStreamId(requestMessage.getSenderInputStreamId());
        message.setReceiverOutputStreamId(FILE_IS_DOWNLOADED);
        return message;
    }

    public static Message getFileTransferErrorResponseMessageFromServer(Message fileMessage) {
        Message message = new Message();
        message.setType(FILE_MESSAGE_RESPONSE);
        message.setReceiverName(fileMessage.getReceiverName());
        message.setSenderInputStreamId(fileMessage.getSenderInputStreamId());
        message.setReceiverOutputStreamId(FILE_TRANSFER_ERROR);
        return message;
    }

    public static Message getFileTransferErrorRequestMessageFromClient(Message fileMessage) {
        Message message = new Message();
        message.setType(FILE_MESSAGE_REQUEST);
        message.setData(fileMessage.getData());
        message.setSenderName(fileMessage.getSenderName());
        message.setReceiverName(fileMessage.getReceiverName());
        message.setSenderInputStreamId(FILE_TRANSFER_ERROR);
        message.setReceiverOutputStreamId(fileMessage.getReceiverOutputStreamId());
        return message;
    }

    public static Message getFileTransferErrorResponseMessageFromClient(Message fileMessage) {
        Message message = new Message();
        message.setType(FILE_MESSAGE_RESPONSE);
        message.setData(fileMessage.getData());
        message.setSenderName(fileMessage.getSenderName());
        message.setReceiverName(fileMessage.getReceiverName());
        message.setSenderInputStreamId(fileMessage.getSenderInputStreamId());
        message.setReceiverOutputStreamId(FILE_TRANSFER_ERROR);
        return message;
    }

    public static Message getFileCancelResponseMessage(Message fileMessage) {
        Message message = new Message();
        message.setType(FILE_MESSAGE_RESPONSE);
        message.setSenderName(fileMessage.getSenderName());
        message.setReceiverName(fileMessage.getReceiverName());
        message.setSenderInputStreamId(fileMessage.getSenderInputStreamId());
        message.setReceiverOutputStreamId(FILE_CANCEL);
        return message;
    }
}
