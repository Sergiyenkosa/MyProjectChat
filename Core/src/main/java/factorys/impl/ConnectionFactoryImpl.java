package factorys.impl;

import connection.Connection;
import factorys.ConnectionFactory;
import org.springframework.context.ApplicationContext;

import java.net.Socket;

/**
 * Created by s.sergienko on 20.01.2017.
 */
public class ConnectionFactoryImpl implements ConnectionFactory{
    private final ApplicationContext context;

    public ConnectionFactoryImpl(ApplicationContext context) {
        this.context = context;
    }

    @Override
    public Connection getConnection(Socket socket) {
        return context.getBean(Connection.class, socket);
    }
}
