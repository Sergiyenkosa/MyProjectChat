package web.configuration;

import javax.websocket.server.ServerEndpointConfig.Configurator;

/**
 * Created by s.sergienko on 23.03.2017.
 */
public class ChatServerEndPointConfigurator extends Configurator {

	private ChatServerEndPoint chatServer = new ChatServerEndPoint();

	@Override
	public <T> T getEndpointInstance(Class<T> endpointClass)
			throws InstantiationException {
		return (T)chatServer;
	}
}
