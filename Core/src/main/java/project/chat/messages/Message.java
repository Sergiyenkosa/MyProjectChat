package project.chat.messages;

import java.io.Serializable;

/**
 * Created by s.sergienko on 17.01.2017.
 */
public class Message implements Serializable {
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
}
