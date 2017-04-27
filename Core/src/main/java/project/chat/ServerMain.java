package project.chat;

import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import project.chat.server.Server;

/**
 * Created by s.sergienko on 20.04.2017.
 */
public class ServerMain {
    public static void main(String[] args) {
        ApplicationContext context
                = new ClassPathXmlApplicationContext("spring/core-context.xml");

        Server server = (Server) context.getBean("server");

        server.runServer();
    }
}
