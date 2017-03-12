package messages;

import java.io.Serializable;

/**
 * Created by s.sergienko on 17.01.2017.
 */
public class Message implements Serializable {
    public static final int FILE_IS_DOWNLOADED = 0;
    public static final int FILE_IS_UPLOADED = 0;
    public static final int FILE_TRANSFER_ERROR = -1;
    public static final int FILE_CANCEL = -2;
    
    private MessageType type;
    private String data;
    private String senderName;
    private String receiverName;
    private byte[] bytes;
    private int senderInputStreamId;
    private int receiverOutputStreamId;

    public MessageType getType() {
        return type;
    }

    public void setType(MessageType type) {
        this.type = type;
    }

    public String getData() {
        return data;
    }

    public void setData(String data) {
        this.data = data;
    }

    public String getSenderName() {
        return senderName;
    }

    public void setSenderName(String senderName) {
        this.senderName = senderName;
    }

    public String getReceiverName() {
        return receiverName;
    }

    public void setReceiverName(String receiverName) {
        this.receiverName = receiverName;
    }

    public int getSenderInputStreamId() {
        return senderInputStreamId;
    }

    public void setSenderInputStreamId(int senderInputStreamId) {
        this.senderInputStreamId = senderInputStreamId;
    }

    public int getReceiverOutputStreamId() {
        return receiverOutputStreamId;
    }

    public void setReceiverOutputStreamId(int receiverOutputStreamId) {
        this.receiverOutputStreamId = receiverOutputStreamId;
    }

    public byte[] getBytes() {
        return bytes;
    }

    public void setBytes(byte[] bytes) {
        this.bytes = bytes;
    }

    public enum MessageType {
        NAME_REQUEST, PASSWORD_REQUEST, USER_NAME, USER_PASSWORD,
        NAME_ACCEPTED, TEXT_MESSAGE, INFO_MESSAGE, ERROR_MESSAGE,
        USER_ADDED, USER_REMOVED, PRIVATE_MESSAGE, FILE_MESSAGE,
        FILE_MESSAGE_FOR_ALL, FILE_MESSAGE_REQUEST,
        FILE_MESSAGE_RESPONSE
    }

//    public static Message getNameRequestMessage() {
//        Message message = new Message();
//        message.type = NAME_REQUEST;
//        return message;
//    }
//
//    public static Message getUserNameMessage(String userName) {
//        Message message = new Message();
//        message.type = USER_NAME;
//        message.data = userName;
//        return message;
//    }
//
//    public static Message getNameAcceptedMessage() {
//        Message message = new Message();
//        message.type = NAME_ACCEPTED;
//        return message;
//    }
//
//    public static Message getUserAddedMessage(String userName) {
//        Message message = new Message();
//        message.type = USER_ADDED;
//        message.data = userName;
//        return message;
//    }
//
//    public static Message getUserRemovedMessage(String userName) {
//        Message message = new Message();
//        message.type = USER_REMOVED;
//        message.data = userName;
//        return message;
//    }
//
//    public static Message getSystemMessage(String systemMessage) {
//        Message message = new Message();
//        message.type = SYSTEM_MESSAGE;
//        message.data = systemMessage;
//        return message;
//    }
//
//    public static Message getErrorMessage(String errorMessage) {
//        Message message = new Message();
//        message.type = ERROR_MESSAGE;
//        message.data = errorMessage;
//        return message;
//    }
//
//    public static Message getTextMessage(String textMessage) {
//        Message message = new Message();
//        message.type = TEXT_MESSAGE;
//        message.data = textMessage;
//        return message;
//    }
//
//    public static Message getPrivateMessage(String privateMessage, String receiverName) {
//        Message message = new Message();
//        message.type = PRIVATE_MESSAGE;
//        message.data = privateMessage;
//        message.receiverName = receiverName;
//        return message;
//    }
//
//    public static Message getFileMessageForeAll(String fileName, byte[] bytes) {
//        Message message = new Message();
//        message.type = FILE_MESSAGE_FOR_ALL;
//        message.data = fileName;
//        message.bytes = bytes;
//        return message;
//    }
//
//    public static Message getFileMessage(String fileName, String receiverName, int senderFileId) {
//        Message message = new Message();
//        message.type = FILE_MESSAGE;
//        message.data = fileName;
//        message.receiverName = receiverName;
//        message.senderFileId = senderFileId;
//        return message;
//    }
//
//    public static Message getFileMessageAnswer(Message fileMessage, int receiverFileId) {
//        Message message = new Message();
//        message.type = FILE_MESSAGE_RESPONSE;
//        message.data = fileMessage.data;
//        message.senderName = fileMessage.senderName;
//        message.receiverName = fileMessage.receiverName;
//        message.senderFileId = fileMessage.senderFileId;
//        message.receiverFileId = receiverFileId;
//        return message;
//    }
//
//    public static Message getFileMessageRequest(Message responseMessage, byte[] bytes) {
//        Message message = new Message();
//        message.type = FILE_MESSAGE_REQUEST;
//        message.data = responseMessage.data;
//        message.senderName = responseMessage.senderName;
//        message.receiverName = responseMessage.receiverName;
//        message.senderFileId = responseMessage.senderFileId;
//        message.receiverFileId = responseMessage.receiverFileId;
//        message.bytes = bytes;
//        return message;
//    }
//
//    public static Message getFileMessageResponse(Message responseMessage) {
//        Message message = new Message();
//        message.type = FILE_MESSAGE_REQUEST;
//        message.data = responseMessage.data;
//        message.senderName = responseMessage.senderName;
//        message.receiverName = responseMessage.receiverName;
//        message.senderFileId = responseMessage.senderFileId;
//        message.receiverFileId = responseMessage.receiverFileId;
//        return message;
//    }
//
//    public static Message getFileIsUploadedRequestMessage(Message responseMessage, byte[] bytes) {
//        Message message = new Message();
//        message.type = FILE_MESSAGE_REQUEST;
//        message.data = responseMessage.data;
//        message.senderName = responseMessage.senderName;
//        message.receiverName = responseMessage.receiverName;
//        message.senderFileId = FILE_IS_UPLOADED;
//        message.receiverFileId = responseMessage.receiverFileId;
//        message.bytes = bytes;
//        return message;
//    }
//
//    public static Message getFileTransferErrorResponseMessageFromServer(Message fileMessage) {
//        Message message = new Message();
//        message.type = FILE_MESSAGE_RESPONSE;
//        message.receiverName = fileMessage.receiverName;
//        message.senderFileId = fileMessage.senderFileId;
//        message.receiverFileId = FILE_TRANSFER_ERROR;
//        return message;
//    }
//
//    public static Message getFileTransferErrorResponseMessageFromClient(Message fileMessage) {
//        Message message = new Message();
//        message.type = FILE_MESSAGE_RESPONSE;
//        message.data = fileMessage.data;
//        message.senderName = fileMessage.senderName;
//        message.receiverName = fileMessage.receiverName;
//        message.senderFileId = fileMessage.senderFileId;
//        message.receiverFileId = FILE_TRANSFER_ERROR;
//        return message;
//    }
//
//    public static Message getFileCancelResponseMessage(Message fileMessage) {
//        Message message = new Message();
//        message.type = FILE_MESSAGE_RESPONSE;
//        message.senderName = fileMessage.senderName;
//        message.receiverName = fileMessage.receiverName;
//        message.senderFileId = fileMessage.senderFileId;
//        message.receiverFileId = FILE_CANCEL;
//        return message;
//    }
//
//    public static Message getFileTransferErrorRequestMessageFromServer(Message fileMessage) {
//        Message message = new Message();
//        message.type = FILE_MESSAGE_REQUEST;
//        message.senderName = fileMessage.senderName;
//        message.senderFileId = FILE_TRANSFER_ERROR;
//        message.receiverFileId = fileMessage.receiverFileId;
//        return message;
//    }
}
