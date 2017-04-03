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
        CREDENTIALS_REQUEST, USER_CREDENTIALS, USER_ACCEPTED,
        TEXT_MESSAGE, INFO_MESSAGE, ERROR_MESSAGE, USER_ADDED,
        USER_REMOVED, PRIVATE_MESSAGE, FILE_MESSAGE,
        FILE_MESSAGE_FOR_ALL, FILE_MESSAGE_REQUEST,
        FILE_MESSAGE_RESPONSE
    }
}
