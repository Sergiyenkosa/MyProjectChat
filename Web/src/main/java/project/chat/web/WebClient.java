package project.chat.web;

import com.google.gson.Gson;
import org.apache.log4j.Logger;
import project.chat.configuration.ChatServerWebClientConfigurator;
import project.chat.connection.Connection;
import project.chat.messages.Message;
import project.chat.messages.MessageType;

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
@ServerEndpoint(value="/chat", configurator=ChatServerWebClientConfigurator.class)
public class WebClient {
    private static final Logger log = Logger.getLogger(WebClient.class);
    private volatile boolean clientConnected = false;
    private Connection  connection;
    private Session userSession;
    private final Gson gson = new Gson();

    public WebClient(Connection connection) {
        this.connection = connection;
    }

    @OnOpen
    public void onOpen(Session userSession) throws IOException, ClassNotFoundException {
        userSession.setMaxTextMessageBufferSize(50000000);

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
            log.error(e.getMessage());
        }
    }

    @OnMessage
    public void onMessage(String s, Session userSession) {
        try {
            Message message = gson.fromJson(s, Message.class);

            connection.send(message);
        } catch (IOException e) {
            Message message = new Message();
            message.setType(MessageType.ERROR_MESSAGE);
            message.setData("Server is temporarily unavailable");

            try {
                userSession.getBasicRemote().sendText(gson.toJson(message));
            } catch (IOException e1) {
                log.error(e1.getMessage());
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
