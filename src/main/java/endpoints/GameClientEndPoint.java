package endpoints;

import client.ClientMessageHandler;

import javax.websocket.*;
import javax.websocket.Session;
import javax.websocket.WebSocketContainer;
import java.net.URI;

@ClientEndpoint
public class GameClientEndPoint {
    Session userSession = null;

    private IMessageHandler messageHandler;

    public GameClientEndPoint(URI endpointURI) {
        try {
            WebSocketContainer container = ContainerProvider.getWebSocketContainer();
            container.connectToServer(this, endpointURI);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @OnOpen
    public void onOpen(Session userSession) {
        System.out.println("opening websocket");
        this.userSession = userSession;
    }

    @OnClose
    public void onClose(Session userSession, CloseReason reason) {
        System.out.println("closing websocket");
        this.userSession = null;
    }

    @OnMessage
    public void onMessage(String message, Session session) {
        if (this.messageHandler != null) {
            this.messageHandler.handleMessage(message, session);
        }
    }

    public void addMessageHandler(IMessageHandler messageHandler) { this.messageHandler = messageHandler; }
}
