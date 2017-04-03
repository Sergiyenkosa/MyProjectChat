package connection.impl;

import connection.Connection;
import messages.Message;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.net.SocketAddress;

/**
 * Created by s.sergienko on 11.10.2016.
 */
public class ConnectionImpl implements Connection {
    private final Socket socket;
    private final ObjectOutputStream out;
    private final ObjectInputStream in;

    public ConnectionImpl(Socket socket) throws IOException {
        this.socket = socket;
        out = new ObjectOutputStream(socket.getOutputStream());
        in = new ObjectInputStream(socket.getInputStream());
    }

    @Override
    public void send(Message message) throws IOException {
        synchronized (out) {
            out.writeObject(message);
            out.flush();
        }
    }

    @Override
    public Message receive() throws IOException, ClassNotFoundException {
        synchronized (in) {
            return (Message) in.readObject();
        }
    }

    @Override
    public SocketAddress getRemoteSocketAddress() {
        return socket.getRemoteSocketAddress();
    }

    @Override
    public Socket getSocket() {
        return socket;
    }


    @Override
    public void close() throws IOException {
        in.close();
        out.flush();
        out.close();
        socket.close();
    }
}
