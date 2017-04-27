package project.chat.messages;

import static project.chat.messages.MessageType.*;

/**
 * Created by s.sergienko on 01.03.2017.
 */
public class MessageFactory {
    public static Message getCredentialsRequestMessage() {
        Message message = new Message();
        message.setType(CREDENTIALS_REQUEST);
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
}
