package connection;

import messages.Message;

import java.io.Closeable;
import java.io.IOException;
import java.net.Socket;
import java.net.SocketAddress;

/**
 * Created by s.sergienko on 17.01.2017.
 */
public interface Connection extends Closeable {
    void send(Message message) throws IOException;

    Message receive() throws IOException, ClassNotFoundException;

    SocketAddress getRemoteSocketAddress();

    Socket getSocket();
}
