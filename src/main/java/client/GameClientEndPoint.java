package client;

import org.json.JSONObject;
import shared.IMessageHandler;

import javax.websocket.*;
import javax.websocket.Session;
import javax.websocket.WebSocketContainer;
import java.io.IOException;
import java.net.URI;

@ClientEndpoint
public class GameClientEndPoint {
    private Session userSession = null;

    private IMessageHandler messageHandler;

    GameClientEndPoint(URI endpointURI) {
        boolean failed = true;
        int triesLeft = 3;
        while (triesLeft > 0 && failed) {
            try {
                WebSocketContainer container = ContainerProvider.getWebSocketContainer();
                container.connectToServer(this, endpointURI);
                failed = false;
            } catch (Exception e) {
                triesLeft--;
                if (triesLeft > 0)
                    System.out.println("Couldn't connect to " + GameClient.hostAddress + ":" + GameClient.port + ". Retrying... " + triesLeft + " tries left");
            }
        }
        if (failed) {
            System.out.println("Couldn't connect to host. Closing application...");
            System.exit(0);
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

//        System.out.println(message);

        if (this.messageHandler != null) {
            this.messageHandler.handleMessage(message, session);
        }
    }

    void sendMessage(JSONObject json) {
        try {
            userSession.getBasicRemote().sendText(json.toString());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    void addMessageHandler(IMessageHandler messageHandler) { this.messageHandler = messageHandler; }
}
