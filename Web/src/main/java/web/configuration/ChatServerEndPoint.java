package web.configuration;

import connection.Connection;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import web.WebClient;

import javax.websocket.OnClose;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.ServerEndpoint;
import java.io.IOException;
import java.net.Socket;

/**
 * Created by s.sergienko on 23.03.2017.
 */
@ServerEndpoint(value="/chat", configurator=ChatServerEndPointConfigurator.class)
public class ChatServerEndPoint {

    @OnOpen
    public void onOpen(Session userSession) throws IOException, ClassNotFoundException {
        userSession.setMaxTextMessageBufferSize(50000000);
        ApplicationContext context
                = new ClassPathXmlApplicationContext("spring/web-context.xml");
        Connection connection = (Connection) context.getBean("connection");

        WebClient webClient = new WebClient(connection, userSession);
        userSession.addMessageHandler(webClient);

        webClient.start();
    }

    @OnClose
    public void onClose(Session userSession) {
        WebClient webClient;
        if (userSession.getMessageHandlers().iterator().hasNext()) {
            webClient = (WebClient) userSession.getMessageHandlers().iterator().next();

            try(Socket socket = webClient.getConnection().getSocket()) {
                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }

            webClient.interrupt();
        }
    }
}
