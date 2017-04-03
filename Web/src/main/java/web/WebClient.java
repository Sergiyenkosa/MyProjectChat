package web;

import com.google.gson.Gson;
import connection.Connection;
import messages.Message;
import messages.MessageFactory;

import javax.websocket.MessageHandler;
import javax.websocket.Session;
import java.io.IOException;

/**
 * Created by s.sergienko on 15.03.2017.
 */
public class WebClient extends Thread implements MessageHandler.Whole<String>{
    private final Connection  connection;
    private final Session webSocketSession;

    public WebClient(Connection connection, Session webSocketSession) {
        this.connection = connection;
        this.webSocketSession = webSocketSession;
    }

    public Connection getConnection() {
        return connection;
    }

    public void run() {
        while (!isInterrupted()) {
            try {
                Message message = connection.receive();
                System.out.println(message.getType());

                webSocketSession.getBasicRemote().sendText(new Gson().toJson(message));
            } catch (IOException | ClassNotFoundException e) {
                break;
            }
        }
    }

    @Override
    public void onMessage(String s) {
        try {
            System.out.println(s);

            Message message = new Gson().fromJson(s, Message.class);

            System.out.println(message.getType());

            connection.send(message);
        } catch (IOException e) {
            try (Session session = webSocketSession){
                session.getBasicRemote().sendText(new Gson().toJson(
                        MessageFactory.getErrorMessage("Server is temporarily unavailable")));
            } catch (IOException e1) {
                e1.printStackTrace();
            }
        }
    }
}
