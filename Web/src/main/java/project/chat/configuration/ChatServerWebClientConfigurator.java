package project.chat.configuration;

import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import project.chat.connection.Connection;
import project.chat.web.WebClient;

import javax.websocket.server.ServerEndpointConfig;

/**
 * Created by s.sergienko on 24.04.2017.
 */
public class ChatServerWebClientConfigurator extends ServerEndpointConfig.Configurator {
    private final ApplicationContext context
            = new ClassPathXmlApplicationContext("spring/web-context.xml");

    @Override
    public <T> T getEndpointInstance(Class<T> endpointClass) throws InstantiationException {
        Connection connection = (Connection) context.getBean("connection");

        return (T) new WebClient(connection);
    }
}