package factorys;

import connection.Connection;

import java.net.Socket;

/**
 * Created by s.sergienko on 20.01.2017.
 */
public interface ConnectionFactory {
    Connection getConnection(Socket socket);
}
