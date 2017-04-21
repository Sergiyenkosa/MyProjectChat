package web;

import com.google.gson.Gson;
import connection.Connection;
import messages.Message;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import javax.websocket.OnClose;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.ServerEndpoint;
import java.io.IOException;
import java.net.Socket;

/**
 * Created by s.sergienko on 15.03.2017.
 */
@ServerEndpoint(value="/chat")
public class WebClient {
    private volatile boolean clientConnected = false;
    private Connection  connection;
    private Session userSession;
    private final Gson gson = new Gson();

    @OnOpen
    public void onOpen(Session userSession) throws IOException, ClassNotFoundException {
        userSession.setMaxTextMessageBufferSize(50000000);
        ApplicationContext context
                = new ClassPathXmlApplicationContext("spring/web-context.xml");
        this.connection = (Connection) context.getBean("connection");

        this.userSession = userSession;

        clientConnected = true;

        new WebClientThread().start();
    }

    @OnClose
    public void onClose(Session userSession) {
        clientConnected = false;

        try (Socket socket = connection.getSocket()){
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @OnMessage
    public void onMessage(String s, Session userSession) {
        try {
            Message message = gson.fromJson(s, Message.class);

            connection.send(message);
        } catch (IOException e) {
            Message message = new Message();
            message.setType(Message.MessageType.ERROR_MESSAGE);
            message.setData("Server is temporarily unavailable");

            try {
                userSession.getBasicRemote().sendText(gson.toJson(message));
            } catch (IOException e1) {
                e1.printStackTrace();
            }
        }
    }

    private class WebClientThread extends Thread {
        public void run() {
            while (clientConnected) {
                try {
                    Message message = connection.receive();

                    userSession.getBasicRemote().sendText(gson.toJson(message));
                    userSession.getBasicRemote().flushBatch();
                } catch (IOException | ClassNotFoundException e) {
                    break;
                }
            }
        }
    }
}
